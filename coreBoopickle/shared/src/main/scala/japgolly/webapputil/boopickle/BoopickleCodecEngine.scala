package japgolly.webapputil.boopickle

import boopickle.{PickleImpl, Pickler, UnpickleImpl}
import japgolly.webapputil.binary._

object BoopickleCodecEngine {

  object pickler extends CodecEngine[Pickler, Throwable] {

    override def encode[A](a: A)(implicit p: Pickler[A]): BinaryData = {
      val bb = PickleImpl.intoBytes(a)
      BinaryData.unsafeFromByteBuffer(bb)
    }

    override def decode[A](b: BinaryData)(implicit p: Pickler[A]): Either[Throwable, A] =
      try {
        val a = UnpickleImpl(p).fromBytes(b.unsafeByteBuffer)
        Right(a)
      } catch {
        case t: Throwable =>
          Left(t)
      }
  }

  object safePickler extends CodecEngine[SafePickler, SafePickler.DecodingFailure] {
    override def encode[A](a: A)(implicit p: SafePickler[A]): BinaryData =
      p.encode(a)

    override def decode[A](b: BinaryData)(implicit p: SafePickler[A]): SafePickler.Result[A] =
      p.decode(b)
  }

}
