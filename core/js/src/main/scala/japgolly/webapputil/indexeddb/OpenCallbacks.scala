package japgolly.webapputil.indexeddb

import japgolly.scalajs.react.Callback
import org.scalajs.dom._

/** Callbacks to install when opening a DB.
 *
 * Note 1: On `versionChange`, the DB connection will be closed automatically.
 *
 * Note 2: There's no `blocked` handler because we currently don't allow blocking. To quote the idb spec:
 *         if "there are open connections that don’t close in response to a versionchange event, the request will be
 *         blocked until all they close".
 */
final case class OpenCallbacks(upgradeNeeded: VersionChange => Callback,
                               versionChange: VersionChange => Callback = _ => Callback.empty,
                               closed       : Callback                  = Callback.empty)

/** Provided when the version of the existing IndexedDB database is outdated. */
final case class VersionChange(db: DatabaseInVersionChange, oldVersion: Int, newVersion: Option[Int]) {

  def createObjectStore[K, V](defn: ObjectStoreDef[K, V], createdInDbVer: Int): Callback =
    Callback.when(oldVersion < createdInDbVer && newVersion.exists(_ >= createdInDbVer))(
      db.createObjectStore(defn))
}

/** Provides legal DB methods available during a version-change callback. */
final class DatabaseInVersionChange(raw: IDBDatabase) {

  def createObjectStore[K, V](defn: ObjectStoreDef[K, V]): Callback =
    Callback {
      raw.createObjectStore(defn.name)
    }
}
