package japgolly.webapputil.indexeddb

import japgolly.scalajs.react._
import japgolly.webapputil.indexeddb.TxnMode._
import japgolly.webapputil.indexeddb.dsl._
import org.scalajs.dom._
import scala.scalajs.js

final class Database(val raw: IDBDatabase, onClose: Callback) {

  def atomic[K, V](store: ObjectStoreDef.Async[K, V]): AtomicAsyncDsl[K, V] =
    new AtomicAsyncDsl(this, store)

  def close: Callback = {
    val actuallyClose =
      Callback(raw.close()).attempt

    // The onclose event handler is only fired "when the database is unexpectedly closed".
    // Therefore we call it here explicitly.
    // https://developer.mozilla.org/en-US/docs/Web/API/IDBDatabase/onclose
    actuallyClose >> onClose
  }

  def compareAndSet(stores: ObjectStoreDef[_, _]*): CasDsl1 =
    new CasDsl1(this, stores)

  def transactionRO: RunTxnDsl1[RO] =
    new RunTxnDsl1(raw, TxnDslRO, IDBTransactionMode.readonly)

  def transactionRW: RunTxnDsl1[RW] =
    new RunTxnDsl1(raw, TxnDslRW, IDBTransactionMode.readwrite)

  // ===================================================================================================================
  // Convenience methods

  /** Note: insert only */
  def add[K, V](store: ObjectStoreDef.Async[K, V])(key: K, value: V): AsyncCallback[Unit] =
    store.encode(value).flatMap(add(store.sync)(key, _))

  /** Note: insert only */
  def add[K, V](store: ObjectStoreDef.Sync[K, V])(key: K, value: V): AsyncCallback[Unit] =
    transactionRW(store)(_.objectStore(store).flatMap(_.add(key, value)))

  /** Note: insert only */
  def addWhenDefined[K, V](store: ObjectStoreDef.Async[K, V])(key: K, value: Option[V]): AsyncCallback[Unit] =
    AsyncCallback.traverseOption_(value)(add(store)(key, _))

  /** Note: insert only */
  def addWhenDefined[K, V](store: ObjectStoreDef.Sync[K, V])(key: K, value: Option[V]): AsyncCallback[Unit] =
    AsyncCallback.traverseOption_(value)(add(store)(key, _))

  def clear[K, V](store: ObjectStoreDef[K, V]): AsyncCallback[Unit] =
    transactionRW(store)(_.objectStore(store.sync).flatMap(_.clear))

  def delete[K, V](store: ObjectStoreDef[K, V])(key: K): AsyncCallback[Unit] =
    transactionRW(store)(_.objectStore(store.sync).flatMap(_.delete(key)))

  def get[K, V](store: ObjectStoreDef.Async[K, V])(key: K): AsyncCallback[Option[V]] =
    get(store.sync)(key).flatMap(AsyncCallback.traverseOption(_)(_.decode))

  def get[K, V](store: ObjectStoreDef.Sync[K, V])(key: K): AsyncCallback[Option[V]] =
    transactionRO(store)(_.objectStore(store).flatMap(_.get(key)))

  def getAllKeys[K, V](store: ObjectStoreDef[K, V]): AsyncCallback[Vector[K]] =
    transactionRO(store)(_.objectStore(store.sync).flatMap(_.getAllKeys))

  def getAllValues[K, V](store: ObjectStoreDef.Async[K, V]): AsyncCallback[Vector[V]] =
    getAllValues(store.sync).flatMap(AsyncCallback.traverse(_)(_.decode))

  def getAllValues[K, V](store: ObjectStoreDef.Sync[K, V]): AsyncCallback[Vector[V]] =
    transactionRO(store)(_.objectStore(store).flatMap(_.getAllValues))

  def openKeyCursor[K, V](store: ObjectStoreDef.Sync[K, V])
                         (use: KeyCursor.ForStore[K] => Callback): AsyncCallback[Unit] =
    transactionRO(store)(_.objectStore(store).flatMap(_.openKeyCursor(use)))

  def openKeyCursorWithRange[K, V](store: ObjectStoreDef.Sync[K, V])
                                  (keyRange: KeyRange.Dsl[K] => KeyRange, dir: js.UndefOr[IDBCursorDirection] = ())
                                  (use: KeyCursor.ForStore[K] => Callback): AsyncCallback[Unit] =
    transactionRO(store)(_.objectStore(store).flatMap(_.openKeyCursorWithRange(keyRange, dir)(use)))

  /** aka upsert */
  def put[K, V](store: ObjectStoreDef.Async[K, V])(key: K, value: V): AsyncCallback[Unit] =
    store.encode(value).flatMap(put(store.sync)(key, _))

  /** aka upsert */
  def put[K, V](store: ObjectStoreDef.Sync[K, V])(key: K, value: V): AsyncCallback[Unit] =
    transactionRW(store)(_.objectStore(store).flatMap(_.put(key, value)))

  /** aka upsert or delete */
  def putOrDelete[K, V](store: ObjectStoreDef.Async[K, V])(key: K, value: Option[V]): AsyncCallback[Unit] =
    value match {
      case Some(v) => put(store)(key, v)
      case None    => delete(store)(key)
    }

  /** aka upsert or delete */
  def putOrDelete[K, V](store: ObjectStoreDef.Sync[K, V])(key: K, value: Option[V]): AsyncCallback[Unit] =
    value match {
      case Some(v) => put(store)(key, v)
      case None    => delete(store)(key)
    }

  /** aka upsert */
  def putWhenDefined[K, V](store: ObjectStoreDef.Async[K, V])(key: K, value: Option[V]): AsyncCallback[Unit] =
    AsyncCallback.traverseOption_(value)(put(store)(key, _))

  /** aka upsert */
  def putWhenDefined[K, V](store: ObjectStoreDef.Sync[K, V])(key: K, value: Option[V]): AsyncCallback[Unit] =
    AsyncCallback.traverseOption_(value)(put(store)(key, _))
}
