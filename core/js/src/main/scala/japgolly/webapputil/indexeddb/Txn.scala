package japgolly.webapputil.indexeddb

import cats.Monad
import japgolly.webapputil.indexeddb.TxnMode._

/** Embedded language for safely working with(in) an IndexedDB transaction.
  *
  * This is necessary because whilst all the transaction methods are async, any other type of asynchronicity is not
  * supported and will result in IndexedDB automatically committing and closing the transaction, in which case,
  * further interaction with the transaction will result in a runtime error.
  *
  * Therefore, returning [[AsyncCallback]] from within transactions is dangerous because it allows composition of
  * both kinds of asynchronicity. To avoid this, we use this embedded language and don't publicly expose its
  * interpretation/translation to [[AsyncCallback]]. From the call-site's point of view, a `Txn[A]` is completely
  * opaque.
  *
  * This also has a nice side-effect of ensuring that transaction completion is always awaited because we do it in the
  * transaction functions right after interpretation. Otherwise, the call-sites would always need to remember to do it
  * if live transaction access were exposed.
  *
  * @tparam A The return type.
  */
sealed trait Txn[+A] { self =>
  import TxnStep._

  type Mode <: TxnMode
  type Self[+B] <: Txn.WithMode[Mode, B] { type Self[+C] = self.Self[C] }

  protected implicit def autoWrapStep[B](step: TxnStep[Mode, B]): Self[B]

  def step: TxnStep[Mode, A]

  final def map[B](f: A => B): Self[B] =
    Map(step, f)

  final def flatMap[B](f: A => Self[B]): Self[B] =
    FlatMap(step, f.andThen(_.step))

  final def >>[B](f: Self[B]): Self[B] = {
    val next = f.step
    FlatMap[Mode, A, B](step, _ => next)
  }

  final def void: Self[Unit] =
    Map[Mode, A, Unit](step, _ => ())
}

object Txn {
  type WithMode[M <: TxnMode, +A] = Txn[A] { type Mode = M }
}

// =====================================================================================================================

/** Embedded language for safely working with(in) a read-write IndexedDB transaction.
  *
  * This is necessary because whilst all the transaction methods are async, any other type of asynchronicity is not
  * supported and will result in IndexedDB automatically committing and closing the transaction, in which case,
  * further interaction with the transaction will result in a runtime error.
  *
  * Therefore, returning [[AsyncCallback]] from within transactions is dangerous because it allows composition of
  * both kinds of asynchronicity. To avoid this, we use this embedded language and don't publicly expose its
  * interpretation/translation to [[AsyncCallback]]. From the call-site's point of view, a `Txn[A]` is completely
  * opaque.
  *
  * This also has a nice side-effect of ensuring that transaction completion is always awaited because we do it in the
  * transaction functions right after interpretation. Otherwise, the call-sites would always need to remember to do it
  * if live transaction access were exposed.
  *
  * @tparam A The return type.
  */
final case class TxnRW[+A](step: TxnStep[RW, A]) extends Txn[A] {
  override type Self[+B] = TxnRW[B]
  override type Mode = RW
  override protected implicit def autoWrapStep[B](s: TxnStep[Mode, B]) = TxnRW(s)
}

object TxnRW {
  implicit lazy val catsInstance: Monad[TxnRW] = new Monad[TxnRW] {
    override def pure[A](a: A) = TxnDslRW.pure(a)
    override def map[A, B](fa: TxnRW[A])(f: A => B) = fa map f
    override def flatMap[A, B](fa: TxnRW[A])(f: A => TxnRW[B]) = fa flatMap f
    override def tailRecM[A, B](a: A)(f: A => TxnRW[Either[A, B]]) = TxnDslRW.tailRec(a)(f)
  }
}
// =====================================================================================================================

/** Embedded language for safely working with(in) a read-only IndexedDB transaction.
  *
  * This is necessary because whilst all the transaction methods are async, any other type of asynchronicity is not
  * supported and will result in IndexedDB automatically committing and closing the transaction, in which case,
  * further interaction with the transaction will result in a runtime error.
  *
  * Therefore, returning [[AsyncCallback]] from within transactions is dangerous because it allows composition of
  * both kinds of asynchronicity. To avoid this, we use this embedded language and don't publicly expose its
  * interpretation/translation to [[AsyncCallback]]. From the call-site's point of view, a `Txn[A]` is completely
  * opaque.
  *
  * This also has a nice side-effect of ensuring that transaction completion is always awaited because we do it in the
  * transaction functions right after interpretation. Otherwise, the call-sites would always need to remember to do it
  * if live transaction access were exposed.
  *
  * @tparam A The return type.
  */
final case class TxnRO[+A](step: TxnStep[RO, A]) extends Txn[A] {
  override type Self[+B] = TxnRO[B]
  override type Mode = RO
  override protected implicit def autoWrapStep[B](s: TxnStep[Mode, B]) = TxnRO(s)

  def rw: TxnRW[A] =
    TxnRW(step)
}

object TxnRO {
  implicit lazy val catsInstance: Monad[TxnRO] = new Monad[TxnRO] {
    override def pure[A](a: A) = TxnDslRO.pure(a)
    override def map[A, B](fa: TxnRO[A])(f: A => B) = fa map f
    override def flatMap[A, B](fa: TxnRO[A])(f: A => TxnRO[B]) = fa flatMap f
    override def tailRecM[A, B](a: A)(f: A => TxnRO[Either[A, B]]) = TxnDslRO.tailRec(a)(f)
  }
}
