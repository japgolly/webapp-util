package japgolly.webapputil.boopickle

import boopickle.{PickleImpl, Pickler, UnpickleImpl}
import japgolly.webapputil.webworker.WebWorkerProtocol
import org.scalajs.dom.Transferable
import scala.scalajs.js
import scala.scalajs.js.typedarray.TypedArrayBufferOps._
import scala.scalajs.js.typedarray._

object BinaryWebWorkerProtocol extends WebWorkerProtocol {
  override type Encoded    = ArrayBuffer
  override type Decoder[A] = Pickler[A]
  override type Encoder[A] = Pickler[A]

  override def encode[A: Pickler](input: A): ArrayBuffer = {
    val bb  = PickleImpl.intoBytes(input)
    val len = bb.limit()
    val ia  = bb.typedArray().subarray(0, len)
    val ab  = ia.buffer.slice(0, len)
    ab
  }

  override def decode[A: Pickler](encoded: ArrayBuffer): A = {
    val bb = TypedArrayBuffer wrap encoded
    UnpickleImpl[A].fromBytes(bb)
  }

  // https://developers.google.com/web/updates/2011/12/Transferable-Objects-Lightning-Fast
  override def transferables(e: ArrayBuffer): js.UndefOr[js.Array[Transferable]] =
    js.Array(e: Transferable)
}
