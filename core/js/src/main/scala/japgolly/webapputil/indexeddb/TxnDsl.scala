package japgolly.webapputil.indexeddb

import cats.{Applicative, Traverse}
import japgolly.scalajs.react._
import japgolly.scalajs.react.util.Util.{identity => identityFn}
import japgolly.webapputil.indexeddb.IndexedDb.ObjectStore
import japgolly.webapputil.indexeddb.TxnMode._
import japgolly.webapputil.indexeddb.{Txn => TxnBase}
import scala.collection.BuildFrom

sealed abstract class TxnDsl {

  type Mode <: TxnMode
  type Txn[+A] <: TxnBase.WithMode[Mode, A] { type Self[+B] = Txn[B] }

  protected implicit def autoWrapStep[B](step: TxnStep[Mode, B]): Txn[B]
  protected implicit def autoWrapStepRO[B](step: TxnStep[RO, B]): Txn[B]
  protected implicit def applicative: Applicative[Txn]

  final def pure[A](a: A): Txn[A] =
    TxnStep.Eval(CallbackTo.pure(a))

  @inline final def delay[A](a: => A): Txn[A] =
    eval(CallbackTo(a))

  final def suspend[A](a: => Txn[A]): Txn[A] =
    TxnStep.Suspend(CallbackTo(a.step))

  // Sync only. Async not allowed by IndexedDB.
  final def eval[A](c: CallbackTo[A]): Txn[A] =
    TxnStep.Eval(c)

  final def unit: Txn[Unit] =
    TxnStep.unit

  @inline final def none: Txn[Option[Nothing]] =
    pure(None)

  final def tailRec[A, B](a: A)(f: A => Txn[Either[A, B]]): Txn[B] =
    TxnStep.TailRec(a, f.andThen(_.step))

  final def objectStore[K, V](s: ObjectStoreDef.Sync[K, V]): Txn[ObjectStore[K, V]] =
    TxnStep.GetStore(s)

  @inline final def objectStore[K, V](s: ObjectStoreDef.Async[K, V]): Txn[ObjectStore[K, s.Value]] =
    objectStore(s.sync)

  @inline final def sequence[G[_], A](txns: G[Txn[A]])(implicit G: Traverse[G]): Txn[G[A]] =
    traverse(txns)(identityFn)

  @inline final def sequenceIterable[F[x] <: Iterable[x], A](txns: => F[Txn[A]])(implicit cbf: BuildFrom[F[Txn[A]], A, F[A]]): Txn[F[A]] =
    traverseIterable(txns)(identityFn)

  @inline final def sequenceIterable_(txns: => Iterable[Txn[Any]]): Txn[Unit] =
    traverseIterable_(txns)(identityFn)

  @inline final def sequenceOption[A](o: => Option[Txn[A]]): Txn[Option[A]] =
    traverseOption(o)(identityFn)

  @inline final def sequenceOption_(o: Option[Txn[Any]]): Txn[Unit] =
    traverseOption_(o)(identityFn)

  final def traverse[G[_], A, B](ga: G[A])(f: A => Txn[B])(implicit G: Traverse[G]): Txn[G[B]] =
    G.traverse(ga)(f.andThen(_.step))

  final def traverseIterable[F[x] <: Iterable[x], A, B](fa: => F[A])(f: A => Txn[B])(implicit cbf: BuildFrom[F[A], B, F[B]]): Txn[F[B]] =
    suspend {
      val as = fa
      val b = cbf.newBuilder(as)

      if (as.isEmpty)
        pure(b.result())
      else
        as.iterator.map(f(_).map(b += _)).reduce(_ >> _) >> delay(b.result())
    }

  final def traverseIterable_[A](fa: => Iterable[A])(f: A => Txn[Any]): Txn[Unit] =
    suspend {
      val as = fa
      val it = as.iterator
      if (it.isEmpty)
        unit
      else {
        val first = f(it.next())
        it.foldLeft(first)(_ >> f(_)).void
      }
    }

  final def traverseOption[A, B](o: => Option[A])(f: A => Txn[B]): Txn[Option[B]] =
    suspend {
      o match {
        case Some(a) => f(a).map(Some(_))
        case None    => none
      }
    }

  final def traverseOption_[A, B](o: => Option[A])(f: A => Txn[B]): Txn[Unit] =
    suspend {
      o match {
        case Some(a) => f(a).void
        case None    => unit
      }
    }
}

// =====================================================================================================================

object TxnDsl {

  object RO extends TxnDsl {
    override type Mode = RO
    override type Txn[+A] = TxnRO[A]
    override protected implicit def autoWrapStep[B](s: TxnStep[Mode, B]) = TxnRO(s)
    override protected implicit def autoWrapStepRO[B](s: TxnStep[RO, B]) = TxnRO(s)
    override protected implicit def applicative = TxnRO.catsInstance
  }

  // =====================================================================================================================

  object RW extends TxnDsl {
    override type Mode = RW
    override type Txn[+A] = TxnRW[A]
    override protected implicit def autoWrapStep[B](s: TxnStep[Mode, B]) = TxnRW(s)
    override protected implicit def autoWrapStepRO[B](s: TxnStep[RO, B]) = TxnRW(s)
    override protected implicit def applicative = TxnRW.catsInstance
  }

}
