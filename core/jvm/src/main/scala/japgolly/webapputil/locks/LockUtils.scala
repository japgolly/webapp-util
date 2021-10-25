package japgolly.webapputil.locks

import japgolly.webapputil.general.Effect
import java.util.concurrent.locks.Lock

object LockUtils {

  def inMutex[A](lock: Lock)(a: => A): A = {
    lock.lockInterruptibly()
    try a finally lock.unlock()
  }

  def maybeInMutex[A](mutex: Option[Lock])(a: => A): A =
    mutex.fold(a)(inMutex(_)(a))

  def inMutexF[F[_], A](lock: Lock)(fa: F[A])(implicit F: Effect[F]): F[A] = {
    val start = F.delay(lock.lockInterruptibly())
    val stop  = F.delay(lock.unlock())
    F.bracket(start)(use = _ => fa)(release = _ => stop)
  }

  def maybeInMutexF[F[_]: Effect, A](mutex: Option[Lock])(io: F[A]): F[A] =
    mutex.fold(io)(inMutexF(_)(io))
}
