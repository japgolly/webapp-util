package japgolly.webapputil.locks

import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.StampedLock

trait SharedLock extends SharedLock_PlatformShared.Unsafe

object SharedLock extends SharedLock_PlatformShared.ObjectUnsafe {

  def apply(): SharedLock =
    Stamped.write()

  object ReadWrite {
    def apply(): ReadWrite =
      Stamped.readWrite()
  }

  // ===================================================================================================================
  object Stamped {
    def read(s: StampedLock = new StampedLock()): SharedLock =
      this(s, Access.Read)

    def write(s: StampedLock = new StampedLock()): SharedLock =
      this(s, Access.Write)

    def readWrite(r: StampedLock = new StampedLock(), w: StampedLock = new StampedLock()): ReadWrite =
      ReadWrite(readLock = read(r), writeLock = write(w))

    private def apply(s: StampedLock, a: Access): SharedLock =
      new Lock(a(s))

    private trait StampedApi extends Generic[Long, Long] {
      def show(): String
      def unlock(stamp: Long): Unit
    }

    private final class Lock(s: StampedApi) extends SharedLock {
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

    private sealed trait Access {
      def apply(s: StampedLock): StampedApi
    }

    private object Access {

      case object Read extends Access {
        override def apply(s: StampedLock): StampedApi = new StampedApi {
          override def lock()                        = s.readLock()
          override def lockInterruptibly()           = s.readLockInterruptibly()
          override def tryLock()                     = s.tryReadLock()
          override def tryLock(t: Long, u: TimeUnit) = s.tryReadLock(t, u)
          override def unlock(i: Long)               = s.unlockRead(i)
          override def show()                        = s"SharedLock.read($s)"
        }
      }

      case object Write extends Access {
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
  }
  // ===================================================================================================================

  case class ReadWrite(override val readLock : SharedLock,
                       override val writeLock: SharedLock)
      extends Generic.ReadWrite[DefaultOnLock, DefaultOnTryLock] {
    override def toString = s"SharedLock.ReadWrite($readLock, $writeLock)"
  }
}
