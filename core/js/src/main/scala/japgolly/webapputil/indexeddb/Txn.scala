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
final case class Txn[+M <: TxnMode, +A](step: TxnStep[M, A]) { self =>
  import TxnStep._

  def map[B](f: A => B): Txn[M, B] =
    Txn(Map(step, f))

  def void: Txn[M, Unit] =
    map(_ => ())
}

object Txn {

  @inline implicit final class InvariantOps[M <: TxnMode, A](private val self: Txn[M, A]) extends AnyVal {
    import TxnStep._

    def flatMap[N <: TxnMode, B](f: A => Txn[N, B])(implicit m: TxnMode.Merge[M, N]): Txn[m.Mode, B] = {
      val step = FlatMap[m.Mode, A, B](m.substM(self.step), a => m.substN(f(a).step))
      Txn(step)
    }

    @inline def unless(cond: Boolean)(implicit ev: TxnStep[RO, Option[Nothing]] => Txn[M, Option[Nothing]]): Txn[M, Option[A]] =
      when(!cond)

    @inline def unless_(cond: Boolean)(implicit ev: TxnStep[RO, Unit] => Txn[M, Unit]): Txn[M, Unit] =
      when_(!cond)

    def when(cond: Boolean)(implicit ev: TxnStep[RO, Option[Nothing]] => Txn[M, Option[Nothing]]): Txn[M, Option[A]] =
      if (cond) self.map(Some(_)) else TxnStep.none

    def when_(cond: Boolean)(implicit ev: TxnStep[RO, Unit] => Txn[M, Unit]): Txn[M, Unit] =
      if (cond) self.void else TxnStep.unit

    def >>[N <: TxnMode, B](f: Txn[N, B])(implicit m: TxnMode.Merge[M, N]): Txn[m.Mode, B] = {
      val next = m.substN(f.step)
      val step = FlatMap[m.Mode, A, B](m.substM(self.step), _ => next)
      Txn(step)
    }
  }

  type CatsInstance[M <: TxnMode] = Monad[Txn[M, *]]

  def catsInstance[M <: TxnMode](dsl: TxnDsl[M]): CatsInstance[M] =
    new CatsInstance[M] {
      override def pure[A](a: A) = dsl.pure(a)
      override def map[A, B](fa: Txn[M, A])(f: A => B) = fa map f
      override def flatMap[A, B](fa: Txn[M, A])(f: A => Txn[M, B]) = fa flatMap f
      override def tailRecM[A, B](a: A)(f: A => Txn[M, Either[A, B]]) = dsl.tailRec(a)(f)
    }

  implicit def catsInstanceRO: CatsInstance[RO] = catsInstance(TxnDslRO)
  implicit def catsInstanceRW: CatsInstance[RW] = catsInstance(TxnDslRW)
}
