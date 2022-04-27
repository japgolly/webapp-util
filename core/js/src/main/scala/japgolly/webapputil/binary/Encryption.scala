package japgolly.webapputil.binary

import japgolly.scalajs.react.AsyncCallback

/** A means of binary encryption and decryption. */
final case class Encryption(encrypt: BinaryData => AsyncCallback[BinaryData],
                            decrypt: BinaryData => AsyncCallback[BinaryData])

object Encryption {

  trait Engine {
    def apply(symmetricKey: BinaryData): AsyncCallback[Encryption]
  }
}
