package japgolly.webapputil.general

import japgolly.scalajs.react._
import japgolly.webapputil.ajax.AjaxException
import japgolly.webapputil.general.CallbackHelpers._

final class AsyncFunction[-I, +E, +O](private[AsyncFunction] val run: I => AsyncCallback[Either[E, O]]) {

  def apply(input: I): AsyncCallback[Either[E, O]] =
    run(input)

  def contramap[A](f: A => I): AsyncFunction[A, E, O] =
    new AsyncFunction[A, E, O](run compose f)

  def contramapSync[A, II <: I](f: A => CallbackTo[II]): AsyncFunction[A, E, O] =
    contramapAsync(f(_).asAsyncCallback)

  def contramapAsync[A, II <: I](f: A => AsyncCallback[II]): AsyncFunction[A, E, O] =
    new AsyncFunction(f(_).flatMap(run))

  def map[A](f: O => A): AsyncFunction[I, E, A] =
    new AsyncFunction(run(_).map(_.map(f)))

  def mapSync[A](f: O => CallbackTo[A]): AsyncFunction[I, E, A] =
    mapAsync(f(_).asAsyncCallback)

  def mapAsync[A](f: O => AsyncCallback[A]): AsyncFunction[I, E, A] =
    new AsyncFunction(run(_).rightFlatMap(f))

  def emap[A](f: E => A): AsyncFunction[I, A, O] =
    new AsyncFunction(run(_).map(_.leftMap(f)))

  def emapSync[A](f: E => CallbackTo[A]): AsyncFunction[I, A, O] =
    emapAsync(f(_).asAsyncCallback)

  def emapAsync[A](f: E => AsyncCallback[A]): AsyncFunction[I, A, O] =
    new AsyncFunction(run(_).leftFlatMap(f))

  def mapResult[EE, OO](f: Either[E, O] => Either[EE, OO]): AsyncFunction[I, EE, OO] =
    new AsyncFunction(run(_).map(f))

  def mapCall[EE, OO](f: AsyncCallback[Either[E, O]] => AsyncCallback[Either[EE, OO]]): AsyncFunction[I, EE, OO] =
    new AsyncFunction(f compose run)

  def attempt(implicit ev: AsyncFunction.MergeErrors[Throwable, E]): AsyncFunction[I, ev.E, O] =
    attemptBy[Throwable](identity)

  def attemptBy[A](f: Throwable => A)(implicit ev: AsyncFunction.MergeErrors[A, E]): AsyncFunction[I, ev.E, O] =
    mapCall(_.attempt.map {
      case Right(r@ Right(_)) => r.widen
      case Right(Left(e))     => Left(ev.merge(Right(e)))
      case Left(t)            => Left(ev.merge(Left(f(t))))
    })

  def attemptInto[EE >: E](f: Throwable => EE): AsyncFunction[I, EE, O] =
    mapCall(_.attempt.map {
      case Right(r@ Right(_)) => r.widen
      case Right(l@ Left(_))  => l.widen
      case Left(t)            => Left(f(t))
    })
}

object AsyncFunction {

  @inline implicit final class ErrorEitherOps[I, E, F, O](private val self: AsyncFunction[I, Either[E, F], O]) extends AnyVal {
    def mergeErrors(implicit ev: AsyncFunction.MergeErrors[E, F]): AsyncFunction[I, ev.E, O] =
      self.emap(ev.merge)
  }

  @inline implicit final class OutputEitherOps[I, E, A](private val self: AsyncFunction[I, E, Either[E, A]]) extends AnyVal {
    def extractErrorFromOutput: AsyncFunction[I, E, A] =
      self.mapResult {
        case Right(r@ Right(_)) => r
        case Right(l@ Left(_))  => l
        case l@ Left(_)         => l.widen
      }
  }

  // ===================================================================================================================
  // Static methods

  def apply[I, E, O](run: I => AsyncCallback[Either[E, O]]): AsyncFunction[I, E, O] =
    new AsyncFunction(run)

  def const[E, O](result: Either[E, O]): AsyncFunction[Any, E, O] =
    const(AsyncCallback.pure(result))

  def const[E, O](result: AsyncCallback[Either[E, O]]): AsyncFunction[Any, E, O] =
    new AsyncFunction(_ => result)

  def simple[I, O](f: I => AsyncCallback[O]): AsyncFunction[I, ErrorMsg, O] =
    AsyncFunction[I, ErrorMsg, O] { i =>
      AsyncCallback.suspend(f(i)).attempt.map {
        case Right(o)  => Right(o)
        case Left(err) => Left(throwableToErrorMsg(err))
      }
    }

  def withoutFailure[I, O](run: I => AsyncCallback[O]): AsyncFunction[I, Nothing, O] =
    new AsyncFunction(run(_).map(Right(_)))

  implicit def reusability[I, E, O]: Reusability[AsyncFunction[I, E, O]] =
    Reusability((a, b) => a.run eq b.run)

  def throwableToErrorMsg(t: Throwable): ErrorMsg =
    t match {
      case e: AjaxException if e.isTimeout     => ErrorMsg.ClientSide.serverCallTimeout
      case AjaxException(x) if x.status == 501 => ErrorMsg.ClientSide.noCompatibleServer
      case AjaxException(_)                    => ErrorMsg.ClientSide.errorContactingServer
      case _                                   => ErrorMsg.errorOccurred(t)
    }

  // ===================================================================================================================
  // Typeclasses

  type MergeErrors[-E1, -E2] = japgolly.webapputil.general.internal.MergeErrors[E1, E2]
  val MergeErrors = japgolly.webapputil.general.internal.MergeErrors
}
