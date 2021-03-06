package japgolly.webapputil.general

import japgolly.univeq.UnivEq

trait LazyVal[+A] {
  def value: A
  def valueThreadSafe: A

  override def equals(x: Any) =
    x match {
      case l: LazyVal[Any] => valueThreadSafe == l.valueThreadSafe
      case _               => false
    }
}

object LazyVal {

  def apply[A](a: => A): LazyVal[A] =
    new Lazy(() => a)

  private final class Lazy[+A](initArg: () => A) extends LazyVal[A] {

    // Don't prevent GC of initArg or waste mem propagating the ref
    private[this] var init = initArg

    private[this] var result: A = _

    // Thread-unsafe
    override def value: A = {
      if (init ne null) {
        try
          result = init()
        catch {
          case t: Throwable =>
            init = () => throw t
            throw t
        }
        init = null
      }
      result
    }

    override def valueThreadSafe: A =
      synchronized(value)
  }

  def pure[A](a: A): LazyVal[A] =
    new Pure(a)

  private final class Pure[+A](val value: A) extends LazyVal[A] {
    override def valueThreadSafe = value
  }

  implicit def univEq[A]: UnivEq[LazyVal[A]] = UnivEq.force
}
