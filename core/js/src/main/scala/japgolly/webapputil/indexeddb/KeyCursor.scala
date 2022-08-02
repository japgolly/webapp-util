package japgolly.webapputil.indexeddb

import japgolly.scalajs.react.Callback
import org.scalajs.dom._
import TxnMode._

object KeyCursor {

  type ForStoreRO[K] = Option[KeyCursorRO[K, IDBObjectStore]]

  type ForStoreRW[K, V] = Option[KeyCursorRW[K, V, IDBObjectStore]]

  def ro[K, S](r: IDBCursorReadOnly[S], k: KeyCodec[K]): KeyCursorRO[K, S] =
    new KeyCursorRO[K, S] {
      override val raw = r
      override protected val keyCodec = k
    }

  def rw[K, V, S](r: IDBCursor[S], s: ObjectStoreDef.Sync[K, V]): KeyCursorRW[K, V, S] =
    new KeyCursorRW[K, V, S] {
      override val raw = r
      override protected val keyCodec = s.keyCodec
      override protected val valueCodec = s.valueCodec
    }
}

// =====================================================================================================================

trait KeyCursorRO[K, +Source] {

  val raw: IDBCursorReadOnly[Source]

  protected val keyCodec: KeyCodec[K]

  def advance(count: Int): Txn[RO, Unit] =
    Txn(TxnStep.Eval(Callback(raw.advance(count))))

  /** Advances cursor by one */
  def continue: Txn[RO, Unit] =
    Txn(TxnStep.Eval(Callback(raw.continue())))

  /** Sets cursor to key */
  def continue(key: K): Txn[RO, Unit] =
    Txn(TxnStep.Eval(Callback {
      val rawKey = keyCodec.encode(key)
      raw.continue(rawKey)
    }))

  private def decodeKey(k: IDBKey) =
    keyCodec.decode(IndexedDbKey.fromJs(k))

  @inline def direction: IDBCursorDirection =
    raw.direction

  def key: Txn[RO, K] =
    Txn(TxnStep.Eval(decodeKey(raw.key)))

  def primaryKey: Txn[RO, K] =
    Txn(TxnStep.Eval(decodeKey(raw.primaryKey)))

  @inline def source: Source =
    raw.source
}

// =====================================================================================================================

trait KeyCursorRW[K, V, +Source] extends KeyCursorRO[K, Source] {
  override val raw: IDBCursor[Source]

  protected val valueCodec: ValueCodec[V]

  def delete: Txn[RW, Unit] =
    Txn(TxnStep.rawCall[RW].unit(raw.delete()))

  def update(newValue: V): Txn[RW, K] =
    ??? // valueCodec.encode(newValue).map(raw.update(_)).void
}
