package japgolly.webapputil.indexeddb

import japgolly.scalajs.react._
import org.scalajs.dom._
import scala.scalajs.js
import scala.scalajs.js.|

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

  // Composite steps

  final case class FlatMap[M <: TxnMode, A, B](from: TxnStep[M, A], f: A => TxnStep[M, B])
    extends TxnStep[M, B]

  final case class Map[M <: TxnMode, A, B](from: TxnStep[M, A], f: A => B)
    extends TxnStep[M, B]

  final case class Suspend[M <: TxnMode, A](body: CallbackTo[TxnStep[M, A]])
    extends TxnStep[M, A]

  final case class TailRec[M <: TxnMode, A, B](a: A, f: A => TxnStep[M, Either[A, B]])
    extends TxnStep[M, B]

  // RO steps

  final case class Eval[A](body: CallbackTo[A])
    extends TxnStep[RO, A]

  final case class GetStore[K, V](defn: ObjectStoreDef.Sync[K, V])
    extends TxnStep[RO, ObjectStore[K, V]]

  final case class StoreGet[K, V](store: ObjectStore[K, V], key: IndexedDbKey)
    extends TxnStep[RO, Option[V]]

  final case class StoreGetAllKeys[K, V](store: ObjectStore[K, V])
    extends TxnStep[RO, Vector[K]]

  final case class StoreGetAllVals[K, V](store: ObjectStore[K, V])
    extends TxnStep[RO, Vector[V]]

  final case class OpenKeyCursor[K](store: ObjectStore[K, _],
                                    range: js.UndefOr[IDBKeyRange | IDBKey],
                                    dir  : js.UndefOr[IDBCursorDirection],
                                    use  : KeyCursor.ForStore[K] => Callback)
    extends TxnStep[RO, Unit]

  // RW steps

  final case class StoreAdd(store: ObjectStore[_, _], key: IndexedDbKey, value: IDBValue)
    extends TxnStep[RW, Unit]

  final case class StoreClear(store: ObjectStore[_, _])
    extends TxnStep[RW, Unit]

  final case class StoreDelete[K, V](store: ObjectStore[K, V], key: IndexedDbKey)
    extends TxnStep[RW, Unit]

  final case class StorePut(store: ObjectStore[_, _], key: IndexedDbKey, value: IDBValue)
    extends TxnStep[RW, Unit]

  // ===================================================================================================================

  val none: TxnStep[RO, Option[Nothing]] =
    pure(None)

  def pure[A](a: A): TxnStep[RO, A] =
    Eval(CallbackTo.pure(a))

  val unit: TxnStep[RO, Unit] =
    Eval(Callback.empty)

  def interpret[A](txn: IDBTransaction, step: TxnStep[TxnMode, A]): AsyncCallback[A] =
    AsyncCallback.suspend {
      import InternalUtil._

      val stores = js.Dynamic.literal().asInstanceOf[js.Dictionary[IDBObjectStore]]

      def getStore(s: ObjectStore[_, _]) =
        AsyncCallback.delay(stores.get(s.defn.name).get)

      def interpret[B](step: TxnStep[TxnMode, B]): AsyncCallback[B] = {
        step match {

          case FlatMap(fa, f) =>
            interpret(fa).flatMap(a => interpret(f(a)))

          case StoreGet(s, k) =>
            getStore(s).flatMap { store =>
              asyncRequest(store.get(k.asJs))(_.result).flatMapSync { result =>
                if (js.isUndefined(result))
                  CallbackTo.pure(None)
                else
                  s.defn.valueCodec.decode(result).map(Some(_))
              }
            }

          case Eval(c) =>
            c.asAsyncCallback

          case Suspend(s) =>
            s.asAsyncCallback.flatMap(interpret(_))

          case GetStore(sd) =>
            AsyncCallback.delay {
              val s = txn.objectStore(sd.name)
              stores.put(sd.name, s)
              new ObjectStore(sd)
            }

          case StoreAdd(s, k, v) =>
            getStore(s).flatMap { store =>
              asyncRequest_(store.add(v, k.asJs))
            }

          case StorePut(s, k, v) =>
            getStore(s).flatMap { store =>
              asyncRequest_(store.put(v, k.asJs))
            }

          case Map(fa, f) =>
            interpret(fa).map(f)

          case step: OpenKeyCursor[_] =>
            def typed[K](x: OpenKeyCursor[K]) =
              getStore(x.store).flatMap { store =>
                asyncRequest(store.openKeyCursor(x.range, x.dir)) { req =>
                  val raw = req.result
                  val kc = Option(raw).map(new KeyCursor(_, x.store.defn.keyCodec))
                  x.use(kc).runNow()
                }
              }
            typed(step.asInstanceOf[OpenKeyCursor[Any]])

          case StoreDelete(s, k) =>
            getStore(s).flatMap { store =>
              asyncRequest_(store.delete(k.asJs))
            }

          case StoreGetAllKeys(s) =>
            import s.defn.keyCodec
            getStore(s).flatMap { store =>
              asyncRequest(store.getAllKeys()) { req =>
                val rawKeys = req.result
                Vector.tabulate(rawKeys.length) { i =>
                  val rawKey = rawKeys(i)
                  keyCodec.decode(IndexedDbKey.fromJs(rawKey)).runNow() // safe in asyncRequest onSuccess
                }
              }
            }

          case StoreGetAllVals(s) =>
            import s.defn.valueCodec
            getStore(s).flatMap { store =>
              asyncRequest(store.getAll()) { req =>
                val rawVals = req.result
                Vector.tabulate(rawVals.length) { i =>
                  val rawVal = rawVals(i)
                  valueCodec.decode(rawVal).runNow() // safe in asyncRequest onSuccess
                }
              }
            }

          case StoreClear(s) =>
            getStore(s).flatMap { store =>
              asyncRequest_(store.clear())
            }

          case TailRec(z, f) =>
            AsyncCallback.tailrec(z)(a => interpret(f(a)))
        }
      }

      interpret(step)
    }

}
