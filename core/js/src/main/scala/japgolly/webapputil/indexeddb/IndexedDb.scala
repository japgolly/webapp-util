package japgolly.webapputil.indexeddb

import japgolly.scalajs.react._
import org.scalajs.dom._

object IndexedDb {

  def apply(raw: IDBFactory): IndexedDb =
    new IndexedDb(raw)

  def global(): Option[IndexedDb] =
    try {
      window.indexedDB.toOption.map(apply)
    } catch {
      case _: Throwable => None
    }

  type OnOpen = OpenCallbacks => AsyncCallback[Database]
}

// =====================================================================================================================

final class IndexedDb(val raw: IDBFactory) {
  import IndexedDb._
  import InternalUtil._

  private def versionChange(db: DatabaseInVersionChange, e: IDBVersionChangeEvent): VersionChange =
    VersionChange(db, e.oldVersion.toInt, e.newVersionOption.map(_.toInt))

  def open(name: DatabaseName): OnOpen =
    _open(raw.open(name.value))

  def open(name: DatabaseName, version: Int): OnOpen =
    _open(raw.open(name.value, version))

  private def _open(rawOpen: => IDBOpenDBRequest[IDBDatabase]): OnOpen =
    callbacks => {

      def create(): IDBOpenDBRequest[IDBDatabase] = {
        val r = rawOpen

        // r.onblocked = callbacks.blocked.toJsFn1

        r.onupgradeneeded = e => {
          val db = new DatabaseInVersionChange(r.result)
          val args = versionChange(db, e)
          callbacks.upgradeNeeded(args).runNow()
        }

        r
      }

      asyncRequest(create()) { r =>
        val rawDb = r.result

        rawDb.onversionchange = e => {
          try {
            val args = versionChange(new DatabaseInVersionChange(rawDb), e)
            callbacks.versionChange(args).runNow()
          } finally {
            // We close the DB at the end of this event on matter what so that other connections don't block and we
            // don't have to handle onblocked events.
            rawDb.close()

            // The onclose event handler is only fired "when the database is unexpectedly closed".
            // Therefore we call it here explicitly.
            // https://developer.mozilla.org/en-US/docs/Web/API/IDBDatabase/onclose
            callbacks.closed.runNow()
          }
        }

        rawDb.onclose = _ => {
          callbacks.closed.runNow()
        }

        new Database(rawDb, onClose = callbacks.closed)
      }
    }

  def deleteDatabase(name: DatabaseName): AsyncCallback[Unit] =
    asyncRequest_(raw.deleteDatabase(name.value))
}
