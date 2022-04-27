package japgolly.webapputil.boopickle.test

import japgolly.scalajs.react.AsyncCallback
import japgolly.webapputil.binary.{BinaryData, Encryption}
import japgolly.webapputil.boopickle.EncryptionEngine
import japgolly.webapputil.test.node.TestNode

object TestEncryption {

  lazy val engine: Encryption.Engine =
    EncryptionEngine.from(TestNode.webCrypto)
      .getOrElse(sys error "Node.webCrypto not accepted as an Encryption.Engine")

  def apply(key: BinaryData): AsyncCallback[Encryption] =
    engine(key)

  object UnsafeTypes {
    implicit def binaryDataFromString(str: String): BinaryData = {
      val bytes = str.getBytes
      assert(bytes.length == str.length)
      BinaryData.unsafeFromArray(bytes)
    }
  }
}
