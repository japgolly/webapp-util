package japgolly.webapputil.binary

/** Capability to encode and decode binary data given a codec typeclass `F[_]` */
trait CodecEngine[F[_], +E] { self =>

  def encode[A](a: A)(implicit codec: F[A]): BinaryData
  def decode[A](b: BinaryData)(implicit codec: F[A]): Either[E, A]

  def mapError[X](f: E => X): CodecEngine[F, X] =
    new CodecEngine[F, X] {

      override def encode[A](a: A)(implicit codec: F[A]): BinaryData =
        self.encode(a)

      override def decode[A](b: BinaryData)(implicit codec: F[A]): Either[X, A] =
        self.decode[A](b).left.map(f)
    }
}

object CodecEngine
