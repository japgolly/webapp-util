package japgolly.webapputil.indexeddb

import japgolly.scalajs.react.CallbackTo
import java.util.UUID
import scala.reflect.ClassTag
import scala.scalajs.js

final case class KeyCodec[A](encode: A => IndexedDbKey,
                             decode: IndexedDbKey => CallbackTo[A]) {

  def xmap[B](onDecode: A => B)(onEncode: B => A): KeyCodec[B] =
    // Delegating because decoding can fail and must be wrapped to be pure
    xmapSync(
      a => CallbackTo(onDecode(a)))(
      onEncode)

  def xmapSync[B](onDecode: A => CallbackTo[B])(onEncode: B => A): KeyCodec[B] =
    KeyCodec[B](
      encode = encode compose onEncode,
      decode = decode(_).flatMap(onDecode))
}

object KeyCodec {

  lazy val double: KeyCodec[Double] =
    primative("Double")

  lazy val int: KeyCodec[Int] =
    primative("Int")

  lazy val long: KeyCodec[Long] =
    string.xmap(_.toLong)(_.toString)

  def primative[A](name: String)(implicit ev: A => IndexedDbKey.Typed, ct: ClassTag[A]): KeyCodec[A] =
    apply[A](IndexedDbKey(_), k => CallbackTo(
      (k.value: Any) match {
        case a: A => a
        case x    => throw new js.JavaScriptException(s"Invalid IDB key found. $name expected, got: $x")
      }
    ))

  lazy val string: KeyCodec[String] =
    primative("String")

  lazy val uuid: KeyCodec[UUID] =
    string.xmap(UUID.fromString)(_.toString)

}
