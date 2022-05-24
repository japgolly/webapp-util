package japgolly.webapputil.indexeddb

import japgolly.scalajs.react.Callback
import org.scalajs.dom._

object KeyCursor {
  type ForStore[K] = Option[KeyCursor[K, IDBObjectStore]]
}

final class KeyCursor[K, +Source](val raw: IDBCursorReadOnly[Source], codec: KeyCodec[K]) {

  def advance(count: Int): Callback =
    Callback(raw.advance(count))

  /** Advances cursor by one */
  def continue: Callback =
    Callback(raw.continue())

  /** Sets cursor to key */
  def continue(key: K): Callback = {
    val rawKey = codec.encode(key)
    Callback(raw.continue(rawKey))
  }

  private def decodeKey(k: IDBKey): K =
    codec.decode(IndexedDbKey.fromJs(k)).runNow()

  @inline def direction: IDBCursorDirection =
    raw.direction

  def key: K =
    decodeKey(raw.key)

  def primaryKey: K =
    decodeKey(raw.primaryKey)

  @inline def source: Source =
    raw.source
}
