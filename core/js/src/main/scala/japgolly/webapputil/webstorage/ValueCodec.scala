package japgolly.webapputil.webstorage

import japgolly.scalajs.react.{AsyncCallback, CallbackTo}
import japgolly.webapputil.binary._
import japgolly.webapputil.webstorage.AbstractWebStorage.{Key, Value}

final case class ValueCodec[A](encode: A => CallbackTo[Value],
                               decode: Value => CallbackTo[A]) {

  def xmap[B](onDecode: A => B)(onEncode: B => A): ValueCodec[B] =
    // Delegating because decoding can fail and must be wrapped to be pure
    xmapSync(
      a => CallbackTo(onDecode(a)))(
      b => CallbackTo(onEncode(b)))

  def xmapSync[B](onDecode: A => CallbackTo[B])(onEncode: B => CallbackTo[A]): ValueCodec[B] =
    ValueCodec[B](
      encode = onEncode(_).flatMap(encode),
      decode = decode(_).flatMap(onDecode))

  def async: ValueCodec.Async[A] =
    ValueCodec.Async(
      encode = encode.andThen(_.asAsyncCallback),
      decode = decode.andThen(_.asAsyncCallback))
}

object ValueCodec {

  final case class Async[A](encode: A => AsyncCallback[Value],
                            decode: Value => AsyncCallback[A]) {

    def xmap[B](onDecode: A => B)(onEncode: B => A): Async[B] =
      // Delegating because decoding can fail and must be wrapped to be pure
      xmapAsync(
        a => AsyncCallback.delay(onDecode(a)))(
        b => AsyncCallback.delay(onEncode(b)))

    def xmapAsync[B](onDecode: A => AsyncCallback[B])(onEncode: B => AsyncCallback[A]): Async[B] =
      Async[B](
        encode = onEncode(_).flatMap(encode),
        decode = decode(_).flatMap(onDecode))

    def xmapRaw(afterEncode : (A, Value) => Value,
                beforeDecode: Value => Value): Async[A] =
      Async[A](
        encode = a => encode(a).map(afterEncode(a, _)),
        decode = v => decode(beforeDecode(v)))

    def webStorageKey(key: Key): WebStorageKey.Async[A] =
      WebStorageKey.Async(key, this)
  }

  // ===================================================================================================================
  // Instances

  val string: ValueCodec[String] =
    apply(
      encode = s => CallbackTo.pure(Value(s)),
      decode = r => CallbackTo.pure(r.value))

  lazy val boolean: ValueCodec[Boolean] =
    string.xmap({
      case "1" => true
      case "0" => false
    })({
      case true  => "1"
      case false => "0"
    })

  def binaryString(implicit enc: BinaryString.Encoder): ValueCodec[BinaryString] =
    string.xmap(BinaryString.encoded)(_.encoded)

  def binary(implicit enc: BinaryString.Encoder): ValueCodec[BinaryData] =
    binaryString.xmap(_.binaryValue)(BinaryString.apply)

  object Async {

    def binary(implicit enc: BinaryString.Encoder): ValueCodec.Async[BinaryData] =
      ValueCodec.binary.async

    def binaryFormat[A](fmt: BinaryFormat[A])(implicit enc: BinaryString.Encoder): ValueCodec.Async[A] =
      binary.xmapAsync(fmt.decode)(fmt.encode)
  }

}
