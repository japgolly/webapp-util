package japgolly.webapputil.core.general

import japgolly.scalajs.react._
import scala.util.Try

private[webapputil] object CallbackHelpers {

  @inline final implicit class EitherExt[A, B](private val self: Either[A, B]) extends AnyVal {
    def leftMap[C](f: A => C): Either[C, B] =
      self match {
        case r@ Right(_) => r.widen
        case Left(a)     => Left(f(a))
      }
  }

  @inline final implicit class RightExt[A](private val self: Right[Any, A]) extends AnyVal {
    @inline def widen: Right[Nothing, A] = self.asInstanceOf[Right[Nothing, A]]
  }

  @inline final implicit class LeftExt[A](private val self: Left[A, Any]) extends AnyVal {
    @inline def widen: Left[A, Nothing] = self.asInstanceOf[Left[A, Nothing]]
  }

  final class HelperAsyncCallbackDisj[E, A](private val underlying: (Try[Either[E, A]] => Callback) => Callback) extends AnyVal {
    @inline private def self: AsyncCallback[Either[E, A]] =
      AsyncCallback(underlying)

    def leftFlatFlatMap[F](f: E => AsyncCallback[Either[F, A]]): AsyncCallback[Either[F, A]] =
      self.flatMap {
        case r@ Right(_) => AsyncCallback.pure(r.widen)
        case Left(e)    => f(e)
      }

    def leftFlatMap[F](f: E => AsyncCallback[F]): AsyncCallback[Either[F, A]] =
      leftFlatFlatMap(f.andThen(_.map(Left(_))))

    // def leftFlatTap[F](f: E => AsyncCallback[F]): AsyncCallback[Either[E, A]] =
    //   self.flatMap {
    //     case l@ Left(e) => f(e).ret(l)
    //     case r@ Right(_) => AsyncCallback.pure(r)
    //   }

    // def leftFlatTapSync[F](f: E => CallbackTo[F]): AsyncCallback[Either[E, A]] =
    //   leftFlatTap(f.andThen(_.asAsyncCallback))

    def rightFlatFlatMap[B](f: A => AsyncCallback[Either[E, B]]): AsyncCallback[Either[E, B]] =
      self.flatMap {
        case Right(a)    => f(a)
        case l@ Left(_) => AsyncCallback.pure(l.widen)
      }

    def rightFlatMap[B](f: A => AsyncCallback[B]): AsyncCallback[Either[E, B]] =
      rightFlatFlatMap(f.andThen(_.map(Right(_))))

    // def rightFlatTap[B](f: A => AsyncCallback[B]): AsyncCallback[Either[E, A]] =
    //   self.flatMap {
    //     case r@ Right(a) => f(a).ret(r)
    //     case l@ Left(_) => AsyncCallback.pure(l)
    //   }

    // def rightFlatTapSync[B](f: A => CallbackTo[B]): AsyncCallback[Either[E, A]] =
    //   rightFlatTap(f.andThen(_.asAsyncCallback))
  }

  implicit def HelperAsyncCallbackDisj[E, A](a: AsyncCallback[Either[E, A]]): HelperAsyncCallbackDisj[E, A] =
    new HelperAsyncCallbackDisj(a.completeWith)

}
