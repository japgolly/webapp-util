package japgolly.webapputil.general

import japgolly.scalajs.react.callback._
import scala.util.Try

object Effect_PlatformSpecific {

  object callback extends Effect.Sync[CallbackTo] {

    @inline override def delay[A](a: => A) =
      CallbackTo(a)

    @inline override def pure[A](a: A) =
      CallbackTo.pure(a)

    @inline override def map[A, B](fa: CallbackTo[A])(f: A => B) =
      fa.map(f)

    @inline override def flatMap[A, B](fa: CallbackTo[A])(f: A => CallbackTo[B]) =
      fa.flatMap(f)

    override def bracket[A, B](fa: CallbackTo[A])(use: A => CallbackTo[B])(release: A => CallbackTo[Unit]): CallbackTo[B] =
      fa.flatMap(a => use(a).finallyRun(release(a)))

    override def runSync[A](fa: CallbackTo[A]): A =
      fa.runNow()
  }

  object asyncCallback extends Effect.Async[AsyncCallback] {

    @inline override def delay[A](a: => A) =
      AsyncCallback.delay(a)

    @inline override def pure[A](a: A) =
      AsyncCallback.pure(a)

    @inline override def map[A, B](fa: AsyncCallback[A])(f: A => B) =
      fa.map(f)

    @inline override def flatMap[A, B](fa: AsyncCallback[A])(f: A => AsyncCallback[B]) =
      fa.flatMap(f)

    override def timeoutMs[A](ms: Long)(fa: AsyncCallback[A]): AsyncCallback[Option[A]] =
      fa.timeoutMs(ms.toDouble)

    override def bracket[A, B](fa: AsyncCallback[A])(use: A => AsyncCallback[B])(release: A => AsyncCallback[Unit]): AsyncCallback[B] =
      fa.flatMap(a => use(a).finallyRun(release(a)))

    override def async[A](fa: (Try[A] => Unit) => Unit): AsyncCallback[A] =
      AsyncCallback[A](f => CallbackTo(fa(f(_).runNow())))
  }

}

trait Effect_PlatformSpecific {
  implicit def callback: Effect.Sync[CallbackTo] = Effect_PlatformSpecific.callback
  implicit def asyncCallback: Effect.Async[AsyncCallback] = Effect_PlatformSpecific.asyncCallback
}
