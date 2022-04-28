package japgolly.webapputil.webworker

import org.scalajs.dom.Transferable
import scala.scalajs.js

trait WebWorkerProtocol {

  /** Type of an encoded message */
  type Encoded

  /** Type-class for serialising messages */
  type Encoder[A]

  /** Type-class for deserialising messages */
  type Decoder[A]

  def encode[A: Encoder](input: A): Encoded

  def decode[A: Decoder](encoded: Encoded): A

  def transferables(e: Encoded): js.UndefOr[js.Array[Transferable]]
}

object WebWorkerProtocol {

  type WithEncoded[E] = WebWorkerProtocol { type Encoded = E }

  type WithEncoder[F[_]] = WebWorkerProtocol { type Encoder[A] = F[A] }

  type WithDecoder[F[_]] = WebWorkerProtocol { type Decoder[A] = F[A] }

  type WithCodecs[Enc[_], Dec[_]] = WebWorkerProtocol {
    type Encoder[A] = Enc[A]
    type Decoder[A] = Dec[A]
  }

  type Full[E, Enc[_], Dec[_]] = WebWorkerProtocol {
    type Encoded = E
    type Encoder[A] = Enc[A]
    type Decoder[A] = Dec[A]
  }
}
