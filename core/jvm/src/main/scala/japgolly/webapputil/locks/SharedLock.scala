package japgolly.webapputil.locks

import japgolly.webapputil.general.Effect
import japgolly.webapputil.locks.SharedLock._
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.StampedLock

/** A lock that can be safely shared between threads. */
trait SharedLock extends Generic[Locked, Option[Locked]] {

  def inMutex[A](a: => A): A = {
    val l = lockInterruptibly()
    try a finally l.unlock()
  }

  def inMutexF[F[_], A](fa: F[A])(implicit F: Effect[F]): F[A] =
    F.bracket(F.delay(lockInterruptibly()))(use = _ => fa)(release = l => F.delay(l.unlock()))
}

object SharedLock {

  trait Generic[L, T] {
    def lock(): L
    def lockInterruptibly(): L
    def tryLock(): T
    def tryLock(time: Long, unit: TimeUnit): T
  }

  final case class Locked(unlock: () => Unit)

  def maybeInMutex[A](mutex: Option[SharedLock])(a: => A): A =
    mutex.fold(a)(_.inMutex(a))

  def maybeInMutexF[F[_]: Effect, A](mutex: Option[SharedLock])(fa: F[A]): F[A] =
    mutex.fold(fa)(_.inMutexF(fa))

  // ===================================================================================================================

  def read(s: StampedLock = new StampedLock()): SharedLock =
    stamped(s, StampedLockAccess.Read)

  def write(s: StampedLock = new StampedLock()): SharedLock =
    stamped(s, StampedLockAccess.Write)

  private def stamped(s: StampedLock, a: StampedLockAccess): SharedLock =
    new Stamped(a(s))

  private trait StampedApi extends Generic[Long, Long] {
    def show(): String
    def unlock(stamp: Long): Unit
  }

  private final class Stamped(s: StampedApi) extends SharedLock {
    override def toString = s.show()

    private def wrapLock(stamp: Long): Locked =
      Locked(() => s.unlock(stamp))

    private def wrapTry(stamp: Long): Option[Locked] =
      Option.unless(stamp == 0L)(wrapLock(stamp))

    override def lock()                        = wrapLock(s.lock())
    override def lockInterruptibly()           = wrapLock(s.lockInterruptibly())
    override def tryLock()                     = wrapTry(s.tryLock())
    override def tryLock(t: Long, u: TimeUnit) = wrapTry(s.tryLock(t, u))
  }

  private sealed trait StampedLockAccess {
    def apply(s: StampedLock): StampedApi
  }

  private object StampedLockAccess {

    case object Read extends StampedLockAccess {
      override def apply(s: StampedLock): StampedApi = new StampedApi {
        override def lock()                        = s.readLock()
        override def lockInterruptibly()           = s.readLockInterruptibly()
        override def tryLock()                     = s.tryReadLock()
        override def tryLock(t: Long, u: TimeUnit) = s.tryReadLock(t, u)
        override def unlock(i: Long)               = s.unlockRead(i)
        override def show()                        = s"SharedLock.read($s)"
      }
    }

    case object Write extends StampedLockAccess {
      override def apply(s: StampedLock): StampedApi = new StampedApi {
        override def lock()                        = s.writeLock()
        override def lockInterruptibly()           = s.writeLockInterruptibly()
        override def tryLock()                     = s.tryWriteLock()
        override def tryLock(t: Long, u: TimeUnit) = s.tryWriteLock(t, u)
        override def unlock(i: Long)               = s.unlockWrite(i)
        override def show()                        = s"SharedLock.write($s)"
      }
    }
  }

  // ===================================================================================================================

  object ReadWrite {
    def apply(s: StampedLock = new StampedLock()): ReadWrite =
      new ReadWrite(s)
  }

  final class ReadWrite(s: StampedLock) {
    override def toString = s"SharedLock.ReadWrite($s)"
    val readLock : SharedLock = SharedLock.read(s)
    val writeLock: SharedLock = SharedLock.write(s)
  }
}
