package japgolly.webapputil.general

import japgolly.scalajs.react._
import japgolly.webapputil.ajax.AjaxException
import japgolly.webapputil.general.CallbackHelpers._

final class AsyncFunction[-I, E, O](private[AsyncFunction] val run: I => AsyncCallback[Either[E, O]]) {

  def apply(input: I): AsyncCallback[Either[E, O]] =
    run(input)

  def contramap[A](f: A => I): AsyncFunction[A, E, O] =
    new AsyncFunction[A, E, O](run compose f)

  def contramapC[A, II <: I](f: A => CallbackTo[II]): AsyncFunction[A, E, O] =
    contramapA(f(_).asAsyncCallback)

  def contramapA[A, II <: I](f: A => AsyncCallback[II]): AsyncFunction[A, E, O] =
    new AsyncFunction[A, E, O](f(_).flatMap(run))

  def map[A](f: O => A): AsyncFunction[I, E, A] =
    new AsyncFunction[I, E, A](run(_).map(_.map(f)))

  def mapC[A](f: O => CallbackTo[A]): AsyncFunction[I, E, A] =
    mapA(f(_).asAsyncCallback)

  def mapA[A](f: O => AsyncCallback[A]): AsyncFunction[I, E, A] =
    new AsyncFunction[I, E, A](run(_).rightFlatMap(f))

  def emap[A](f: E => A): AsyncFunction[I, A, O] =
    new AsyncFunction[I, A, O](run(_).map(_.leftMap(f)))

  def emapC[A](f: E => CallbackTo[A]): AsyncFunction[I, A, O] =
    emapA(f(_).asAsyncCallback)

  def emapA[A](f: E => AsyncCallback[A]): AsyncFunction[I, A, O] =
    new AsyncFunction[I, A, O](run(_).leftFlatMap(f))

  def mergeFailure(implicit ev: AsyncFunction.MergeFailure[E, O]): AsyncFunction[I, E, ev.A] = {
    val run2 = ev.apply(this).run
    new AsyncFunction[I, E, ev.A](run2(_).map {
      case Right(r@ Right(_)) => r
      case Right(l@ Left(_))  => l
      case l@ Left(_)         => l.widen
    })
  }
}

object AsyncFunction {

  def apply[I, E, O](run: I => AsyncCallback[Either[E, O]]): AsyncFunction[I, E, O] =
    new AsyncFunction(run)

  def const[E, O](result: Either[E, O]): AsyncFunction[Any, E, O] =
    const(AsyncCallback.pure(result))

  def const[E, O](result: AsyncCallback[Either[E, O]]): AsyncFunction[Any, E, O] =
    new AsyncFunction(_ => result)

  def fromSimple[I, O](f: I => CallbackTo[AsyncCallback[O]]): AsyncFunction[I, ErrorMsg, O] =
    AsyncFunction[I, ErrorMsg, O] { i =>
      f(i).asAsyncCallback.flatten.attempt.map {
        case Right(o)  => Right(o)
        case Left(err) => Left(throwableToErrorMsg(err))
      }
    }

  /** Working around crappy type inference */
  trait MergeFailure[E, O] {
    type A
    def apply[I]: AsyncFunction[I, E, O] => AsyncFunction[I, E, Either[E, A]]
  }
  object MergeFailure {
    implicit def a[E, _A]: MergeFailure[E, Either[E, _A]] { type A = _A } =
      new MergeFailure[E, Either[E, _A]] {
        override type A = _A
        override def apply[I] = identity
      }
  }

  implicit def reusability[I, E, O]: Reusability[AsyncFunction[I, E, O]] =
    Reusability((a, b) => a.run eq b.run)

  // TODO: variance
  implicit def variance[I, E, O, II <: I, EE >: E, OO >: O](a: AsyncFunction[I, E, O]): AsyncFunction[II, EE, OO] =
    new AsyncFunction(a.run)

  def throwableToErrorMsg(t: Throwable): ErrorMsg =
    t match {
      case e: AjaxException if e.isTimeout     => ErrorMsg.ClientSide.serverCallTimeout
      case AjaxException(x) if x.status == 501 => ErrorMsg.ClientSide.noCompatibleServer
      case AjaxException(_)                    => ErrorMsg.ClientSide.errorContactingServer
      case _                                   => ErrorMsg.errorOccurred(t)
    }
}
