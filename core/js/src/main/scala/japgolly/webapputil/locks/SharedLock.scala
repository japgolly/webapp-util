package japgolly.webapputil.locks

import japgolly.scalajs.react.{AsyncCallback, CallbackTo}

final class SharedLock private() extends AbstractSharedLock {

  private var mutex: Option[AsyncCallback.Barrier] =
    None

  override protected def unsafeRelease(): Unit =
    for (m <- mutex) {
      mutex = None
      m.complete.runNow()
    }

  override protected def unsafeTryAcquire() =
    mutex match {

      case None =>
        // Mutex empty
        val b = AsyncCallback.barrier.runNow()
        mutex = Some(b)
        None

      case Some(b) =>
        // Mutex in use
        Some(b.await)
    }
}

// =====================================================================================================================

object SharedLock extends SharedLock_PlatformShared.ObjectSafeF[AsyncCallback] {

  def apply(): SharedLock =
    new SharedLock

  def create: CallbackTo[SharedLock] =
    CallbackTo(apply())

  object ReadWrite {
    def apply(): ReadWrite =
      new ReadWrite()

    def create: CallbackTo[ReadWrite] =
      CallbackTo(apply())
  }

  // ===================================================================================================================

  final class ReadWrite private() extends Generic.ReadWrite[AsyncCallback, DefaultOnLock, DefaultOnTryLock] {

    // Whether it's a read or write mutex is determined by readers being > 0 or not
    private var mutex: Option[AsyncCallback.Barrier] =
      None

    private var readers =
      0

    private def createMutex(): Unit = {
      assert(readers == 0)
      val b = AsyncCallback.barrier.runNow()
      mutex = Some(b)
    }

    private def releaseMutexIfNoReaders(): Unit =
      if (readers == 0)
        for (m <- mutex) {
          mutex = None
          m.complete.runNow()
        }

    override val readLock: AbstractSharedLock =
      new AbstractSharedLock {

        override protected def unsafeRelease(): Unit = {
          assert(readers > 0)
          readers -= 1
          releaseMutexIfNoReaders()
        }

        override protected def unsafeTryAcquire() =
          mutex match {

            case None =>
              // Mutex empty
              createMutex()
              readers = 1
              None

            case Some(b) =>
              if (readers > 0) {
                // Read-mutex in use
                readers += 1
                None

              } else {
                // Write-mutex in use
                Some(b.await)
              }
          }
      }

    override val writeLock: AbstractSharedLock =
      new AbstractSharedLock {

        override protected def unsafeRelease(): Unit =
          releaseMutexIfNoReaders()

        override protected def unsafeTryAcquire() =
          mutex match {

            case None =>
              // Mutex empty
              createMutex()
              None

            case Some(b) =>
              // Mutex in use
              Some(b.await)
          }
      }

    override def toString = s"SharedLock.ReadWrite($readLock, $writeLock)"
  }
}
