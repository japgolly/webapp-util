package japgolly.webapputil.core.entrypoint

import japgolly.webapputil.core.binary.BinaryData
import japgolly.webapputil.core.entrypoint.EntrypointDef._

final case class EntrypointDef[Input](objectName: String)(implicit _codec: Codec[Input]) {

  implicit val codec: Codec[Input] =
    _codec

  val objectAndMethod: String =
    objectName + "." + MainMethodName
}

object EntrypointDef {
  final val MainMethodName = "m"

  trait Codec[A] { self =>
    val decodeOrThrow: String => A
    val encode: A => String
    val escapeEncodedString = true

    def xmap[B](f: A => B)(g: B => A): Codec[B] =
      new Codec[B] {
        override val decodeOrThrow       = s => f(self.decodeOrThrow(s))
        override val encode              = b => self.encode(g(b))
        override val escapeEncodedString = self.escapeEncodedString
      }

    def xmapEncoded(onEncode           : String => String,
                    onDecode           : String => String,
                    escapeEncodedString: Boolean = true): Codec[A] = {
      val _escapeEncodedString = escapeEncodedString
      new Codec[A] {
        override val decodeOrThrow       = s => self.decodeOrThrow(onDecode(s))
        override val encode              = a => onEncode(self.encode(a))
        override val escapeEncodedString = _escapeEncodedString
      }
    }

    def base64: Codec[A] =
      xmapEncoded(
        onEncode            = BinaryData.fromStringAsUtf8(_).toBase64,
        onDecode            = BinaryData.fromBase64(_).toStringAsUtf8,
        escapeEncodedString = false,
      )
  }

  object Codec {
    implicit lazy val unit: Codec[Unit] =
      new Codec[Unit] {
        override val decodeOrThrow       = _ => ()
        override val encode              = _ => ""
        override val escapeEncodedString = false
      }

    object ClearText {
      implicit lazy val int: Codec[Int] =
        new Codec[Int] {
          override val decodeOrThrow       = _.toInt
          override val encode              = _.toString
          override val escapeEncodedString = false
        }

      implicit lazy val long: Codec[Long] =
        new Codec[Long] {
          override val decodeOrThrow       = _.toLong
          override val encode              = _.toString
          override val escapeEncodedString = false
        }

      implicit lazy val short: Codec[Short] =
        new Codec[Short] {
          override val decodeOrThrow       = _.toShort
          override val encode              = _.toString
          override val escapeEncodedString = false
        }

      implicit lazy val byte: Codec[Byte] =
        new Codec[Byte] {
          override val decodeOrThrow       = _.toByte
          override val encode              = _.toString
          override val escapeEncodedString = false
        }

      implicit lazy val float: Codec[Float] =
        new Codec[Float] {
          override val decodeOrThrow       = _.toFloat
          override val encode              = _.toString
          override val escapeEncodedString = false
        }

      implicit lazy val double: Codec[Double] =
        new Codec[Double] {
          override val decodeOrThrow       = _.toDouble
          override val encode              = _.toString
          override val escapeEncodedString = false
        }

      implicit lazy val boolean: Codec[Boolean] =
        new Codec[Boolean] {
          override val decodeOrThrow       = _ == "1"
          override val encode              = b => if (b) "1" else "0"
          override val escapeEncodedString = false
        }

      implicit lazy val string: Codec[String] =
        new Codec[String] {
          override val decodeOrThrow = identity
          override val encode        = identity
        }
    }
  }

}
