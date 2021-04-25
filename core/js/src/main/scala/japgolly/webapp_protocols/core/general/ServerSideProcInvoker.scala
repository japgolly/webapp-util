package japgolly.webapp_protocols.core.general

import japgolly.scalajs.react._
import japgolly.webapp_protocols.core.general.CallbackHelpers._
import org.scalajs.dom.ext.AjaxException

final class ServerSideProcInvoker[-I, E, O](private[ServerSideProcInvoker] val run: I => AsyncCallback[Either[E, O]]) {

  def apply(input: I): AsyncCallback[Either[E, O]] =
    run(input)

  def contramap[A](f: A => I): ServerSideProcInvoker[A, E, O] =
    new ServerSideProcInvoker[A, E, O](run compose f)

  def contramapC[A, II <: I](f: A => CallbackTo[II]): ServerSideProcInvoker[A, E, O] =
    contramapA(f(_).asAsyncCallback)

  def contramapA[A, II <: I](f: A => AsyncCallback[II]): ServerSideProcInvoker[A, E, O] =
    new ServerSideProcInvoker[A, E, O](f(_).flatMap(run))

  def map[A](f: O => A): ServerSideProcInvoker[I, E, A] =
    new ServerSideProcInvoker[I, E, A](run(_).map(_.map(f)))

  def mapC[A](f: O => CallbackTo[A]): ServerSideProcInvoker[I, E, A] =
    mapA(f(_).asAsyncCallback)

  def mapA[A](f: O => AsyncCallback[A]): ServerSideProcInvoker[I, E, A] =
    new ServerSideProcInvoker[I, E, A](run(_).rightFlatMap(f))

  def emap[A](f: E => A): ServerSideProcInvoker[I, A, O] =
    new ServerSideProcInvoker[I, A, O](run(_).map(_.leftMap(f)))

  def emapC[A](f: E => CallbackTo[A]): ServerSideProcInvoker[I, A, O] =
    emapA(f(_).asAsyncCallback)

  def emapA[A](f: E => AsyncCallback[A]): ServerSideProcInvoker[I, A, O] =
    new ServerSideProcInvoker[I, A, O](run(_).leftFlatMap(f))

  def mergeFailure(implicit ev: ServerSideProcInvoker.MergeFailure[E, O]): ServerSideProcInvoker[I, E, ev.A] = {
    val run2 = ev.apply(this).run
    new ServerSideProcInvoker[I, E, ev.A](run2(_).map {
      case Right(r@ Right(_)) => r
      case Right(l@ Left(_))  => l
      case l@ Left(_)         => l.widen
    })
  }
}

object ServerSideProcInvoker {

  def apply[I, E, O](run: I => AsyncCallback[Either[E, O]]): ServerSideProcInvoker[I, E, O] =
    new ServerSideProcInvoker(run)

  def const[E, O](result: Either[E, O]): ServerSideProcInvoker[Any, E, O] =
    const(AsyncCallback.pure(result))

  def const[E, O](result: AsyncCallback[Either[E, O]]): ServerSideProcInvoker[Any, E, O] =
    new ServerSideProcInvoker(_ => result)

  def fromSimple[I, O](f: I => CallbackTo[AsyncCallback[O]]): ServerSideProcInvoker[I, ErrorMsg, O] =
    ServerSideProcInvoker[I, ErrorMsg, O] { i =>
      f(i).asAsyncCallback.flatten.attempt.map {
        case Right(o)  => Right(o)
        case Left(err) => Left(throwableToErrorMsg(err))
      }
    }

  /** Working around crappy type inference */
  trait MergeFailure[E, O] {
    type A
    def apply[I]: ServerSideProcInvoker[I, E, O] => ServerSideProcInvoker[I, E, Either[E, A]]
  }
  object MergeFailure {
    implicit def a[E, _A]: MergeFailure[E, Either[E, _A]] { type A = _A } =
      new MergeFailure[E, Either[E, _A]] {
        override type A = _A
        override def apply[I] = identity
      }
  }

  implicit def reusability[I, E, O]: Reusability[ServerSideProcInvoker[I, E, O]] =
    Reusability((a, b) => a.run eq b.run)

  implicit def variance[I, E, O, II <: I, EE >: E, OO >: O](a: ServerSideProcInvoker[I, E, O]): ServerSideProcInvoker[II, EE, OO] =
    new ServerSideProcInvoker(a.run.andThen(_.widen))

  def throwableToErrorMsg(t: Throwable): ErrorMsg =
    t match {
      case e: AjaxException if e.isTimeout     => ErrorMsg.ClientSide.serverCallTimeout
      case AjaxException(x) if x.status == 501 => ErrorMsg.ClientSide.noCompatibleServer
      case AjaxException(_)                    => ErrorMsg.ClientSide.errorContactingServer
      case _                                   => ErrorMsg.errorOccurred(t)
    }
}
