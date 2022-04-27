package japgolly.webapputil.binary

import scala.scalajs.js
import scala.scalajs.js.typedarray.Uint8Array

/** Binary data efficiently encoded as a UTF-16 string. */
final class BinaryString(val encoded: String)(implicit enc: BinaryString.Encoder) {

  lazy val binaryValue: BinaryData =
    BinaryData.unsafeFromUint8Array(enc.decode(encoded))
}

object BinaryString {

  def encoded(str: String)(implicit enc: Encoder): BinaryString =
    new BinaryString(str)

  def apply(bin: BinaryData)(implicit enc: Encoder): BinaryString =
    encoded(enc.encode(bin.unsafeUint8Array))

  /** It is recommended that you use the JS `base32768` library to back this. */
  trait Encoder {
    def encode(bin: Uint8Array): String
    def decode(str: String): Uint8Array
  }

  // ===================================================================================================================

  object Base32768 {

    def global: Encoder =
      apply(js.Dynamic.global.base32768)

    def apply(jsInstance: Any): Encoder = {
      assert(js.typeOf(jsInstance) == "object", "JS object expected. Got: " + jsInstance)
      val d = jsInstance.asInstanceOf[js.Dynamic]
      assert(js.typeOf(d.encode) == "function", ".encode is not a function")
      assert(js.typeOf(d.decode) == "function", ".decode is not a function")
      force(jsInstance)
    }

    def force(jsInstance: Any): Encoder = {
      val d = jsInstance.asInstanceOf[js.Dynamic]
      new Encoder {
        override def decode(str: String): Uint8Array = d.decode(str).asInstanceOf[Uint8Array]
        override def encode(bin: Uint8Array): String = d.encode(bin).asInstanceOf[String]
      }
    }
  }
}
