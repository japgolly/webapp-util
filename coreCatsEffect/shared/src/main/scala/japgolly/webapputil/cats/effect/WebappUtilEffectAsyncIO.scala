package japgolly.webapputil.cats.effect

import cats.effect.IO
import japgolly.webapputil.general.Effect
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import scala.util.Try

object WebappUtilEffectAsyncIO extends WebappUtilEffectAsyncIO

trait WebappUtilEffectAsyncIO extends Effect.Async[IO] {

  override def delay[A](a: => A): IO[A] =
    IO(a)

  override def pure[A](a: A): IO[A] =
    IO(a)

  override def map[A, B](fa: IO[A])(f: A => B): IO[B] =
    fa.map(f)

  override def flatMap[A, B](fa: IO[A])(f: A => IO[B]): IO[B] =
    fa.flatMap(f)

  override def async[A](f: (Try[A] => Unit) => Unit): IO[A] = {
    val f2: (Either[Throwable, A] => Unit) => Unit = g => f(tryA => g(tryA.toEither))
    IO.async_[A](f2)
  }

  override def timeoutMs[A](d: Long)(fa: IO[A]): IO[Option[A]] =
    fa
      .attempt
      .timeout(FiniteDuration(d, TimeUnit.MILLISECONDS))
      .attempt
      .flatMap {
        case Right(Right(a)) => IO.pure(Some(a))
        case Right(Left(e))  => IO.raiseError(e)
        case Left(_)         => IO.pure(None)
      }
}
