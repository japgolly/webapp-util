package japgolly.webapputil.indexeddb

import japgolly.scalajs.react.{AsyncCallback, CallbackTo}
import japgolly.webapputil.binary._
import java.util.UUID
import org.scalajs.dom.IDBValue
import scala.reflect.ClassTag
import scala.scalajs.js
import scala.scalajs.js.typedarray.ArrayBuffer

final case class ValueCodec[A](encode: A => CallbackTo[IDBValue],
                               decode: IDBValue => CallbackTo[A]) {

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

  type ThisIsBinary = ValueCodec[A] =:= ValueCodec[BinaryData]

  def compress(c: Compression)(implicit ev: ThisIsBinary): ValueCodec[BinaryData] =
    ev(this).xmap(c.decompressOrThrow)(c.compress)
}

object ValueCodec {

  lazy val binary: ValueCodec[BinaryData] =
    apply(
      encode = b => CallbackTo.pure(b.unsafeArrayBuffer),
      decode = d => CallbackTo(BinaryData.unsafeFromArrayBuffer(d.asInstanceOf[ArrayBuffer]))
    )

  lazy val boolean: ValueCodec[Boolean] =
    primative("Boolean")

  lazy val double: ValueCodec[Double] =
    primative("Double")

  lazy val int: ValueCodec[Int] =
    primative("Int")

  lazy val long: ValueCodec[Long] =
    string.xmap(_.toLong)(_.toString)

  def primative[A: ClassTag](name: String): ValueCodec[A] =
    apply(
      encode = a => CallbackTo.pure(a),
      decode = d => CallbackTo(
        (d: Any) match {
          case a: A => a
          case x    => throw new js.JavaScriptException(s"Invalid IDB value found. $name expected, got: $x")
        }
      )
    )

  lazy val string: ValueCodec[String] =
    primative("String")

  lazy val uuid: ValueCodec[UUID] =
    string.xmap(UUID.fromString)(_.toString)

  // ===================================================================================================================

  final case class Async[A](encode: A => AsyncCallback[IDBValue],
                            decode: IDBValue => AsyncCallback[A]) {

    def xmap[B](onDecode: A => B)(onEncode: B => A): Async[B] =
      // Delegating because decoding can fail and must be wrapped to be pure
      xmapAsync(
        a => AsyncCallback.delay(onDecode(a)))(
        b => AsyncCallback.delay(onEncode(b)))

    def xmapAsync[B](onDecode: A => AsyncCallback[B])(onEncode: B => AsyncCallback[A]): Async[B] =
      Async[B](
        encode = onEncode(_).flatMap(encode),
        decode = decode(_).flatMap(onDecode))

    type ThisIsBinary = Async[A] =:= Async[BinaryData]

    def xmapBinaryFormat[B](fmt: BinaryFormat[B])(implicit ev: ThisIsBinary): Async[B] =
      ev(this).xmapAsync(fmt.decode)(fmt.encode)
  }

  object Async {

    lazy val binary: ValueCodec.Async[BinaryData] =
      ValueCodec.binary.async

    def binary[A](fmt: BinaryFormat[A]): ValueCodec.Async[A] =
      binary.xmapBinaryFormat(fmt)
  }
}
