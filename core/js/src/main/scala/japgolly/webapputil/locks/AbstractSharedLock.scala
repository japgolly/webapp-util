package japgolly.webapputil.locks

import japgolly.scalajs.react.callback.AsyncCallback
import japgolly.webapputil.general.Effect
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

trait AbstractSharedLock extends GenericSharedLock.Safe.Default[AsyncCallback] {
  protected def unsafeRelease(): Unit
  /** @return await if already locked */
  protected def unsafeTryAcquire(): Option[AsyncCallback[Unit]]

  protected final type Locked =
    GenericSharedLock.Safe.Locked[AsyncCallback]

  override final protected def F: Effect[AsyncCallback] =
    implicitly

  private val locked: Locked =
    GenericSharedLock.Safe.Locked(F.delay {
      unsafeRelease()
    })

  private def acquire[A](onLock: AsyncCallback[A], onAwait: Option[AsyncCallback[A]]): AsyncCallback[A] = {
    lazy val self: AsyncCallback[A] =
      F.suspend {
        unsafeTryAcquire() match {

          case None =>
            // Lock acquired
            onLock

          case Some(await) =>
            // Mutex in use
            onAwait.getOrElse(F.flatMap(await)(_ => self))
        }
      }
    self
  }

  override val lock: AsyncCallback[Locked] =
    acquire[Locked](
      onLock = F.pure(locked),
      onAwait = None,
    )

  override val lockInterruptibly: AsyncCallback[Locked] =
    lock

  override val tryLock: AsyncCallback[Option[Locked]] =
    acquire[Option[Locked]](
      onLock = F.pure(Some(locked)),
      onAwait = Some(F.pure(None)),
    )

  override def tryLock(time: Long, unit: TimeUnit): AsyncCallback[Option[Locked]] =
    F.suspend {
      type X = AsyncCallback[Option[Locked]]
      var allowRun = true
      val timer: X = AsyncCallback.delay {allowRun = false; None}.delay(FiniteDuration(time, unit))
      val run: X = F.flatMap(lock) { l =>
        if (allowRun)
          F.pure(Some(l))
        else
          F.map(l.unlock)(_ => None)
      }
      timer.race(run).map(_.merge)
    }

  /** not re-entrant */
  override def apply[A](fa: AsyncCallback[A]): AsyncCallback[A] =
    F.flatMap(lock)(l => fa.finallyRun(l.unlock))
}