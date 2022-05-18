package japgolly.webapputil.boopickle

import boopickle._
import japgolly.webapputil.binary._
import japgolly.webapputil.entrypoint._
import java.nio.ByteBuffer

object EntrypointDefExt {
  import EntrypointDef.Codec

  trait Implicits {
    @inline final implicit def EntrypointDefCodecBoopickleExt[A](self: Codec[A]): Implicits.EntrypointDefCodecBoopickleExt[A] =
      new Implicits.EntrypointDefCodecBoopickleExt[A](self)

    @inline final implicit def implicitEntrypointDefCodecViaBoopickle[A](implicit p: Pickler[A]): EntrypointDef.Codec[A] =
      EntrypointDef.Codec.binary.pickle[A]
  }

  object Implicits extends Implicits {
    final class EntrypointDefCodecBoopickleExt[A](private val self: Codec[A]) extends AnyVal {
      type ThisIsBinary = Codec[A] =:= Codec[BinaryData]

      def pickle[B](implicit pickler: Pickler[B], ev: ThisIsBinary): Codec[B] = {
        val unpickle = UnpickleImpl[B]
        ev(self)
          .xmap[ByteBuffer](_.unsafeByteBuffer)(BinaryData.unsafeFromByteBuffer)
          .xmap(unpickle.fromBytes(_))(PickleImpl.intoBytes(_))
      }
    }
  }
}
