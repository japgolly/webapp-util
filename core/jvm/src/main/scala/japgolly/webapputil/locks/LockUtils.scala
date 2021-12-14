package japgolly.webapputil.locks

import japgolly.webapputil.general.Effect
import java.util.concurrent.locks.Lock

object LockUtils {

  def inMutex[A](lock: Lock)(a: => A)(implicit m: LockMechanism): A = {
    m.lock(lock)
    try a finally lock.unlock()
  }

  def maybeInMutex[A](mutex: Option[Lock])(a: => A)(implicit m: LockMechanism): A =
    mutex.fold(a)(inMutex(_)(a))

  def inMutexF[F[_], A](lock: Lock)(fa: F[A])(implicit F: Effect[F], m: LockMechanism): F[A] = {
    val start = F.delay(m.lock(lock))
    val stop  = F.delay(lock.unlock())
    F.bracket(start)(use = _ => fa)(release = _ => stop)
  }

  def maybeInMutexF[F[_]: Effect, A](mutex: Option[Lock])(io: F[A])(implicit m: LockMechanism): F[A] =
    mutex.fold(io)(inMutexF(_)(io))
}
