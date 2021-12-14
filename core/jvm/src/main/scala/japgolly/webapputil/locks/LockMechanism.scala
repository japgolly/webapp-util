package japgolly.webapputil.locks

import java.util.concurrent.locks.Lock
import java.util.concurrent.{TimeUnit, TimeoutException}

sealed trait LockMechanism {
  def lock(l: Lock): Unit
}

object LockMechanism {

  implicit val default: LockMechanism =
    Interruptibly

  case object Interruptibly extends LockMechanism {
    override def lock(l: Lock): Unit =
      l.lockInterruptibly()
  }

  final case class LimitWaitTime(time: Long, unit: TimeUnit, lockName: String = null) extends LockMechanism {
    override def lock(l: Lock): Unit =
      if (!l.tryLock(time, unit)) {
        val name = Option(lockName).fold("lock")(_ + " lock")
        throw new TimeoutException(s"Failed to aquire $name in $time $unit")
      }
  }
}
