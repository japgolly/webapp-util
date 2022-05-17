package japgolly.webapputil.indexeddb

sealed trait TxnMode

object TxnMode {
  sealed trait RW extends TxnMode
  sealed trait RO extends RW

  // ===================================================================================================================

  trait Merge[M <: TxnMode, N <: TxnMode] {
    type Mode <: TxnMode

    def substM[F[+_ <: TxnMode, _], A](f: F[M, A]): F[Mode, A]
    def substN[F[+_ <: TxnMode, _], A](f: F[N, A]): F[Mode, A]
  }

  object Merge {
    type To[M <: TxnMode, N <: TxnMode, R <: TxnMode] = Merge[M, N] { type Mode = R }

    implicit def eql[M <: TxnMode]: To[M, M, M] =
      new Merge[M, M] {
        override type Mode = M
        override def substM[F[+_ <: TxnMode, _], A](f: F[M, A]) = f
        override def substN[F[+_ <: TxnMode, _], A](f: F[M, A]) = f
      }

    implicit def rorw: To[RO, RW, RW] =
      new Merge[RO, RW] {
        override type Mode = RW
        override def substM[F[+_ <: TxnMode, _], A](f: F[RO, A]) = f
        override def substN[F[+_ <: TxnMode, _], A](f: F[RW, A]) = f
      }

    implicit def rwro: To[RW, RO, RW] =
      new Merge[RW, RO] {
        override type Mode = RW
        override def substM[F[+_ <: TxnMode, _], A](f: F[RW, A]) = f
        override def substN[F[+_ <: TxnMode, _], A](f: F[RO, A]) = f
      }
  }
}
