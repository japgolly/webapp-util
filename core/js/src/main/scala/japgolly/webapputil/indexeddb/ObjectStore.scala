package japgolly.webapputil.indexeddb

import japgolly.scalajs.react.Callback
import japgolly.webapputil.indexeddb.TxnMode._
import org.scalajs.dom.IDBCursorDirection
import scala.scalajs.js

final class ObjectStore[K, V](val defn: ObjectStoreDef.Sync[K, V]) {
  import defn.{keyCodec, valueCodec}

  private implicit def autoWrap[M <: TxnMode, A](s: TxnStep[M, A]): Txn[M, A] =
    Txn(s)

  /** Note: insert only */
  def add(key: K, value: V): Txn[RW, Unit] = {
    val k = keyCodec.encode(key)
    TxnDslRW.eval(valueCodec.encode(value)).flatMap(TxnStep.StoreAdd(this, k, _))
  }

  /** Note: insert only */
  def addWhenDefined(key: K, value: Option[V]): Txn[RW, Unit] =
    value.fold[Txn[RW, Unit]](TxnStep.unit)(add(key, _))

  def clear: Txn[RW, Unit] =
    TxnStep.StoreClear(this)

  def delete(key: K): Txn[RW, Unit] =
    TxnStep.StoreDelete(this, keyCodec.encode(key))

  def get(key: K): Txn[RO, Option[V]] =
    TxnStep.StoreGet(this, keyCodec.encode(key))

  def getAllKeys: Txn[RO, Vector[K]] =
    TxnStep.StoreGetAllKeys(this)

  def getAllValues: Txn[RO, Vector[V]] =
    TxnStep.StoreGetAllVals(this)

  def modify(key: K)(f: V => V): Txn[RW, Option[V]] =
    get(key).flatMap {
      case Some(v1) =>
        val v2 = f(v1)
        put(key, v2).map(_ => Some(v2)) // thread-safe cos we're in a locked txn
      case None =>
        TxnDslRW.none
    }

  def modifyOption(key: K)(f: Option[V] => Option[V]): Txn[RW, Option[V]] =
    for {
      o1 <- get(key)
      o2  = f(o1)
      _  <- putOrDelete(key, o2)
    } yield o2

  def openKeyCursor(use: KeyCursor.ForStore[K] => Callback): Txn[RO, Unit] =
    TxnStep.OpenKeyCursor(this, (), (), use)

  def openKeyCursorWithRange(keyRange: KeyRange.Dsl[K] => KeyRange, dir: js.UndefOr[IDBCursorDirection] = ())
                            (use: KeyCursor.ForStore[K] => Callback): Txn[RO, Unit] = {
    val r = keyRange(new KeyRange.Dsl(defn.keyCodec)).raw
    TxnStep.OpenKeyCursor(this, r, dir, use)
  }

  /** aka upsert */
  def put(key: K, value: V): Txn[RW, Unit] = {
    val k = keyCodec.encode(key)
    TxnDslRW.eval(valueCodec.encode(value)).flatMap(TxnStep.StorePut(this, k, _))
  }

  /** aka upsert or delete */
  def putOrDelete(key: K, value: Option[V]): Txn[RW, Unit] =
    value match {
      case Some(v) => put(key, v)
      case None    => delete(key)
    }

  /** aka upsert */
  def putWhenDefined(key: K, value: Option[V]): Txn[RW, Unit] =
    value.fold[Txn[RW, Unit]](TxnStep.unit)(put(key, _))
}
