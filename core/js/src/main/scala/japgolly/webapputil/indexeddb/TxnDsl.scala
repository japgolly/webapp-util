package japgolly.webapputil.indexeddb

import cats.Traverse
import japgolly.scalajs.react._
import japgolly.scalajs.react.util.Util.{identity => identityFn}
import japgolly.webapputil.indexeddb.IndexedDb.ObjectStore
import japgolly.webapputil.indexeddb.TxnMode._
import scala.collection.BuildFrom

sealed abstract class TxnDsl[M <: TxnMode] {

  implicit def catsInstance: Txn.CatsInstance[M]

  protected implicit def autoWrapStepRO[B](step: TxnStep[RO, B]): Txn[M, B]

  private implicit def autoWrapStepM[B](step: TxnStep[M, B]): Txn[M, B] =
    Txn(step)

  // Sync only. Async not allowed by IndexedDB.
  @inline final def eval[A](c: CallbackTo[A]): Txn[M, A] =
    TxnStep.Eval(c)

  final def pure[A](a: A): Txn[M, A] =
    eval(CallbackTo.pure(a))

  @inline final def delay[A](a: => A): Txn[M, A] =
    eval(CallbackTo(a))

  final def unit: Txn[M, Unit] =
    TxnStep.unit

  @inline final def none: Txn[M, Option[Nothing]] =
    pure(None)

  final def suspend[A](a: => Txn[M, A]): Txn[M, A] =
    TxnStep.Suspend(CallbackTo(a.step))

  final def tailRec[A, B](a: A)(f: A => Txn[M, Either[A, B]]): Txn[M, B] =
    TxnStep.TailRec(a, f.andThen(_.step))

  final def objectStore[K, V](s: ObjectStoreDef.Sync[K, V]): Txn[M, ObjectStore[K, V]] =
    TxnStep.GetStore(s)

  @inline final def objectStore[K, V](s: ObjectStoreDef.Async[K, V]): Txn[M, ObjectStore[K, s.Value]] =
    objectStore(s.sync)

  @inline final def sequence[G[_], A](txns: G[Txn[M, A]])(implicit G: Traverse[G]): Txn[M, G[A]] =
    traverse(txns)(identityFn)

  @inline final def sequenceIterable[F[x] <: Iterable[x], A](txns: => F[Txn[M, A]])(implicit cbf: BuildFrom[F[Txn[M, A]], A, F[A]]): Txn[M, F[A]] =
    traverseIterable(txns)(identityFn)

  @inline final def sequenceIterable_(txns: => Iterable[Txn[M, Any]]): Txn[M, Unit] =
    traverseIterable_(txns)(identityFn)

  @inline final def sequenceOption[A](o: => Option[Txn[M, A]]): Txn[M, Option[A]] =
    traverseOption(o)(identityFn)

  @inline final def sequenceOption_(o: Option[Txn[M, Any]]): Txn[M, Unit] =
    traverseOption_(o)(identityFn)

  final def traverse[G[_], A, B](ga: G[A])(f: A => Txn[M, B])(implicit G: Traverse[G]): Txn[M, G[B]] =
    G.traverse(ga)(f.andThen(_.step))

  final def traverseIterable[F[x] <: Iterable[x], A, B](fa: => F[A])(f: A => Txn[M, B])(implicit cbf: BuildFrom[F[A], B, F[B]]): Txn[M, F[B]] =
    suspend {
      val as = fa
      val b = cbf.newBuilder(as)

      if (as.isEmpty)
        pure(b.result())
      else
        as.iterator.map(f(_).map(b += _)).reduce(_ >> _) >> delay(b.result())
    }

  final def traverseIterable_[A](fa: => Iterable[A])(f: A => Txn[M, Any]): Txn[M, Unit] =
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

  final def traverseOption[A, B](o: => Option[A])(f: A => Txn[M, B]): Txn[M, Option[B]] =
    suspend {
      o match {
        case Some(a) => f(a).map(Some(_))
        case None    => none
      }
    }

  final def traverseOption_[A, B](o: => Option[A])(f: A => Txn[M, B]): Txn[M, Unit] =
    suspend {
      o match {
        case Some(a) => f(a).void
        case None    => unit
      }
    }
}

// =====================================================================================================================

object TxnDsl {

  object RO extends TxnDsl[RO] {
    override implicit def catsInstance: Txn.CatsInstance[RO] = Txn.catsInstance(this)
    override protected implicit def autoWrapStepRO[B](s: TxnStep[RO, B]) = Txn(s)
  }

  object RW extends TxnDsl[RW] {
    override implicit def catsInstance: Txn.CatsInstance[RW] = Txn.catsInstance(this)
    override protected implicit def autoWrapStepRO[B](s: TxnStep[RO, B]) = Txn(s)
  }
}
