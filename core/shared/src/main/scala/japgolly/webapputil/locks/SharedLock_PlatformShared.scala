package japgolly.webapputil.locks

import japgolly.webapputil.general.Effect
import java.util.concurrent.TimeUnit

object SharedLock_PlatformShared {

  // ===================================================================================================================
  object ObjectSafe {

    trait Generic[F[_], L, T] {
      protected def F: Effect[F]
      protected def unlock(lock: L): F[Unit]

      val lock: F[L]
      val lockInterruptibly: F[L]
      val tryLock: F[T]
      def tryLock(time: Long, unit: TimeUnit): F[T]

      def apply[A](fa: F[A]): F[A] =
        F.bracket(lockInterruptibly)(use = _ => fa)(release = unlock)
    }

    object Generic {
      trait ReadWrite[F[_], L, T] {
        val readLock: Generic[F, L, T]
        val writeLock: Generic[F, L, T]
      }
    }

    type DefaultOnLock[F[_]]    = Locked[F]
    type DefaultOnTryLock[F[_]] = Option[Locked[F]]

    final case class Locked[F[_]](unlock: F[Unit])
  }

  trait ObjectSafe {
    import SharedLock_PlatformShared.{ObjectSafe => X}
    type Generic[F[_], L, T]    = X.Generic[F, L, T]
    val  Generic                = X.Generic
    type DefaultOnLock[F[_]]    = X.DefaultOnLock[F]
    type DefaultOnTryLock[F[_]] = X.DefaultOnTryLock[F]
    type Locked[F[_]]           = X.Locked[F]
    val  Locked                 = X.Locked
  }

  trait ObjectSafeF[F[_]] {
    import SharedLock_PlatformShared.{ObjectSafe => X}
    type Generic[L, T]    = X.Generic[F, L, T]
    val  Generic          = X.Generic
    type DefaultOnLock    = X.DefaultOnLock[F]
    type DefaultOnTryLock = X.DefaultOnTryLock[F]
    type Locked           = X.Locked[F]
    val  Locked           = X.Locked
  }

  trait Safe[F[_]] extends ObjectSafe.Generic[F, ObjectSafe.DefaultOnLock[F], ObjectSafe.DefaultOnTryLock[F]] {
    override protected def unlock(lock: ObjectSafe.DefaultOnLock[F]): F[Unit] =
      lock.unlock
  }

  // ===================================================================================================================
  object ObjectUnsafe {

    trait Generic[L, T] { self =>
      protected def unlock(lock: L): Unit
      def lock(): L
      def lockInterruptibly(): L
      def tryLock(): T
      def tryLock(time: Long, unit: TimeUnit): T

      def apply[A](a: => A): A = {
        val l = lockInterruptibly()
        try a finally unlock(l)
      }

      def applyF[F[_], A](fa: F[A])(implicit F: Effect[F]): F[A] =
        F.bracket(F.delay(lockInterruptibly()))(use = _ => fa)(release = l => F.delay(unlock(l)))

      def withEffectGeneric[F[_]](implicit E: Effect[F]): ObjectSafe.Generic[F, L, T] =
        new ObjectSafe.Generic[F, L, T] {
          override protected def F                         = E
          override val lock                                = E.delay(self.lock())
          override val lockInterruptibly                   = E.delay(self.lockInterruptibly())
          override val tryLock                             = E.delay(self.tryLock())
          override def tryLock(time: Long, unit: TimeUnit) = E.delay(self.tryLock(time, unit))
          override protected def unlock(lock: L)           = E.delay(self.unlock(lock))
        }
    }

    object Generic {
      trait ReadWrite[L, T] {
        val readLock: Generic[L, T]
        val writeLock: Generic[L, T]
      }
    }

    type DefaultOnLock    = Locked
    type DefaultOnTryLock = Option[Locked]

    final case class Locked(unlock: () => Unit) {
      def unlockF[F[_]](implicit F: Effect[F]): F[Unit] =
        F.delay(unlock())

      def withEffect[F[_]](implicit F: Effect[F]): ObjectSafe.Locked[F] =
        ObjectSafe.Locked[F](unlockF[F])
    }
  }

  trait ObjectUnsafe {
    import SharedLock_PlatformShared.{ObjectUnsafe => X}
    type Generic[L, T]    = X.Generic[L, T]
    val  Generic          = X.Generic
    type DefaultOnLock    = X.DefaultOnLock
    type DefaultOnTryLock = X.DefaultOnTryLock
    type Locked           = X.Locked
    val  Locked           = X.Locked
  }

  trait Unsafe extends ObjectUnsafe.Generic[ObjectUnsafe.DefaultOnLock, ObjectUnsafe.DefaultOnTryLock] { self =>
    import ObjectUnsafe.{DefaultOnLock => L}

    override protected def unlock(lock: L): Unit =
      lock.unlock()

    def withEffect[F[_]](implicit E: Effect[F]): Safe[F] = {
      type L = ObjectSafe.DefaultOnLock[F]
      new Safe[F] {
        override protected def F                         = E
        override val lock                                = E.delay(self.lock().withEffect[F])
        override val lockInterruptibly                   = E.delay(self.lockInterruptibly().withEffect[F])
        override val tryLock                             = E.delay(self.tryLock().map(_.withEffect[F]))
        override def tryLock(time: Long, unit: TimeUnit) = E.delay(self.tryLock(time , unit).map(_.withEffect[F]))
        override protected def unlock(lock: L)           = lock.unlock
      }
    }

  }
}
