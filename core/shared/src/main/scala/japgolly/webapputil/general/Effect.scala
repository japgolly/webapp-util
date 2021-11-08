package japgolly.webapputil.general

import scala.util.Try

trait Effect[F[_]] extends Effect.Monad[F] {
  def bracket[A, B](fa: F[A])(use: A => F[B])(release: A => F[Unit]): F[B]
}

object Effect extends Effect_PlatformSpecific {

  trait Monad[F[_]] {
    def delay  [A]   (a: => A)               : F[A]
    def pure   [A]   (a: A)                  : F[A]
    def map    [A, B](fa: F[A])(f: A => B)   : F[B]
    def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B]

    def flatten[A](ffa: F[F[A]]): F[A] =
      flatMap(ffa)(identity)

    def suspend[A](fa: => F[A]): F[A] =
      flatMap(delay(fa))(identity)
  }

  trait Sync[F[_]] extends Effect[F] {
    def runSync[A](fa: F[A]): A
  }

  trait Async[F[_]] extends Effect[F] {
    def async[A](f: (Try[A] => Unit) => Unit): F[A]

    def timeoutMs[A](ms: Long)(fa: F[A]): F[Option[A]]

    def timeoutMsOrThrow[A](ms: Long, err: => Throwable)(fa: F[A]): F[A] =
      map(
        timeoutMs(ms)(fa)
      )(_.getOrElse(throw err))
  }

}
