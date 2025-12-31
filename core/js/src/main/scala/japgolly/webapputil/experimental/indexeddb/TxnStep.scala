package japgolly.webapputil.experimental.indexeddb

import japgolly.scalajs.react._
import japgolly.webapputil.experimental.indexeddb.IndexedDb.ObjectStore
import org.scalajs.dom._

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
sealed trait TxnStep[+M <: TxnMode, +A]

object TxnStep {
  import TxnMode._

  final case class FlatMap        [M <: TxnMode, A, B](from: TxnStep[M, A], f: A => TxnStep[M, B])                   extends TxnStep[M, B]
  final case class Map            [M <: TxnMode, A, B](from: TxnStep[M, A], f: A => B)                               extends TxnStep[M, B]
  final case class Suspend        [M <: TxnMode, A]   (body: CallbackTo[TxnStep[M, A]])                              extends TxnStep[M, A]
  final case class TailRec        [M <: TxnMode, A, B](a: A, f: A => TxnStep[M, Either[A, B]])                       extends TxnStep[M, B]

  final case class Eval           [A]                 (body: CallbackTo[A])                                          extends TxnStep[RO, A]
  final case class GetStore       [K, V]              (defn: ObjectStoreDef.Sync[K, V])                              extends TxnStep[RO, ObjectStore[K, V]]
  final case class StoreGet       [K, V]              (store: ObjectStore[K, V], key: IndexedDbKey)                  extends TxnStep[RO, Option[V]]
  final case class StoreGetAllKeys[K, V]              (store: ObjectStore[K, V])                                     extends TxnStep[RO, Vector[K]]
  final case class StoreGetAllVals[K, V]              (store: ObjectStore[K, V])                                     extends TxnStep[RO, Vector[V]]

  final case class StoreAdd                           (store: ObjectStore[_, _], key: IndexedDbKey, value: IDBValue) extends TxnStep[RW, Unit]
  final case class StoreClear                         (store: ObjectStore[_, _])                                     extends TxnStep[RW, Unit]
  final case class StoreDelete    [K, V]              (store: ObjectStore[K, V], key: IndexedDbKey)                  extends TxnStep[RW, Unit]
  final case class StorePut                           (store: ObjectStore[_, _], key: IndexedDbKey, value: IDBValue) extends TxnStep[RW, Unit]

  val none: TxnStep[RO, Option[Nothing]] =
    pure(None)

  def pure[A](a: A): TxnStep[RO, A] =
    Eval(CallbackTo.pure(a))

  val unit: TxnStep[RO, Unit] =
    Eval(Callback.empty)
}
