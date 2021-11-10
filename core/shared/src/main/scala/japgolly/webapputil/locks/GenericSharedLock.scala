package japgolly.webapputil.locks

import japgolly.webapputil.general.Effect
import java.util.concurrent.TimeUnit

object GenericSharedLock {

  trait Safe[F[_], L, T] {
    protected def F: Effect[F]
    protected def unlock(lock: L): F[Unit]

    val lock: F[L]
    val lockInterruptibly: F[L]
    val tryLock: F[T]
    def tryLock(time: Long, unit: TimeUnit): F[T]

    def apply[A](fa: F[A]): F[A] =
      F.bracket(lockInterruptibly)(use = _ => fa)(release = unlock)
  }

  object Safe { obj =>

    type DefaultOnLock[F[_]]    = Locked[F]
    type DefaultOnTryLock[F[_]] = Option[Locked[F]]

    final case class Locked[F[_]](unlock: F[Unit])

    trait ReadWrite[F[_], L, T] {
      val readLock: Safe[F, L, T]
      val writeLock: Safe[F, L, T]
    }

    object ReadWrite {
      type Default[F[_]] = ReadWrite[F, DefaultOnLock[F], DefaultOnTryLock[F]]
    }

    trait ExportObject {
      type Generic[F[_], L, T]    = Safe[F, L, T]
      val  Generic                = Safe
      type DefaultOnLock[F[_]]    = obj.DefaultOnLock[F]
      type DefaultOnTryLock[F[_]] = obj.DefaultOnTryLock[F]
      type Locked[F[_]]           = obj.Locked[F]
      val  Locked                 = obj.Locked
    }

    trait ExportObjectF[F[_]] {
      type Generic[L, T]    = Safe[F, L, T]
      val  Generic          = Safe
      type DefaultOnLock    = obj.DefaultOnLock[F]
      type DefaultOnTryLock = obj.DefaultOnTryLock[F]
      type Locked           = obj.Locked[F]
      val  Locked           = obj.Locked
    }

    trait Default[F[_]] extends Safe[F, DefaultOnLock[F], DefaultOnTryLock[F]] {
      override protected def unlock(lock: DefaultOnLock[F]): F[Unit] =
        lock.unlock
    }
  }

  // ===================================================================================================================

  trait Unsafe[L, T] { self =>
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

    def withEffectGeneric[F[_]](implicit E: Effect[F]): Safe[F, L, T] =
      new Safe[F, L, T] {
        override protected def F                         = E
        override val lock                                = E.delay(self.lock())
        override val lockInterruptibly                   = E.delay(self.lockInterruptibly())
        override val tryLock                             = E.delay(self.tryLock())
        override def tryLock(time: Long, unit: TimeUnit) = E.delay(self.tryLock(time, unit))
        override protected def unlock(lock: L)           = E.delay(self.unlock(lock))
      }
  }

  object Unsafe { obj =>

    type DefaultOnLock    = Locked
    type DefaultOnTryLock = Option[Locked]

    final case class Locked(unlock: () => Unit) {
      def unlockF[F[_]](implicit F: Effect[F]): F[Unit] =
        F.delay(unlock())

      def withEffect[F[_]](implicit F: Effect[F]): Safe.Locked[F] =
        Safe.Locked[F](unlockF[F])
    }

    trait ReadWrite[L, T] {
      val readLock: Unsafe[L, T]
      val writeLock: Unsafe[L, T]
    }

    object ReadWrite {
      type Default = ReadWrite[DefaultOnLock, DefaultOnTryLock]
    }

    trait ExportObject {
      type Generic[L, T]    = Unsafe[L, T]
      val  Generic          = Unsafe
      type DefaultOnLock    = obj.DefaultOnLock
      type DefaultOnTryLock = obj.DefaultOnTryLock
      type Locked           = obj.Locked
      val  Locked           = obj.Locked
    }

    trait Default extends Unsafe[DefaultOnLock, DefaultOnTryLock] { self =>
      import Unsafe.{DefaultOnLock => L}

      override protected def unlock(lock: L): Unit =
        lock.unlock()

      def withEffect[F[_]](implicit E: Effect[F]): Safe.Default[F] = {
        type L = Safe.DefaultOnLock[F]
        new Safe.Default[F] {
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
}
