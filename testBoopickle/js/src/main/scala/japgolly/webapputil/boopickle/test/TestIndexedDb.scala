package japgolly.webapputil.boopickle.test

import japgolly.scalajs.react.{AsyncCallback, Callback}
import japgolly.webapputil.indexeddb._
import japgolly.webapputil.test.node.TestNode
import org.scalajs.dom.window
import scala.scalajs.js

object TestIndexedDb {

  /** Loads a local fake-indexeddb JS bundle.
    *
    * @param path Absolute path. eg "/home/me/blah/dist/fake-indexeddb"
    */
  def loadLocalFakeIndexedDbBundle(path: String): IndexedDb = {
    TestNode.require(path)
    js.Dynamic.global.window.indexedDB   = TestNode.node.indexedDB
    js.Dynamic.global.window.IDBKeyRange = TestNode.node.IDBKeyRange
    IndexedDb(window.indexedDB.get)
  }

  // ===================================================================================================================

  private var prevDbIndex = 0

  def freshDbName(): DatabaseName = {
    prevDbIndex += 1
    DatabaseName("testdb_" + prevDbIndex)
  }

  def fresh(onOpen: OpenCallbacks)(implicit idb: IndexedDb): AsyncCallback[Database] = {
    val name = freshDbName()
    idb.open(name)(onOpen)
  }

  def apply(name: String, stores: ObjectStoreDef[_, _]*)(implicit idb: IndexedDb): AsyncCallback[Database] =
    apply(DatabaseName(name), stores: _*)

  def apply(name: DatabaseName, stores: ObjectStoreDef[_, _]*)(implicit idb: IndexedDb): AsyncCallback[Database] =
    idb.open(name)(createStoresOnOpen(stores: _*))

  def apply(stores: ObjectStoreDef[_, _]*)(implicit idb: IndexedDb): AsyncCallback[Database] =
    fresh(createStoresOnOpen(stores: _*))

  def unusedOpenCallbacks: OpenCallbacks =
    OpenCallbacks(
      upgradeNeeded = _ => Callback.empty,
      versionChange = _ => Callback.empty,
      closed        = Callback.empty,
    )

  def createStoresOnOpen(stores: ObjectStoreDef[_, _]*): OpenCallbacks =
    unusedOpenCallbacks.copy(
      upgradeNeeded = e => Callback.traverse(stores)(e.db.createObjectStore(_))
    )

  // ===================================================================================================================

  object UnsafeTypes {
    implicit def autoIndexedDbDatabaseName(s: String): DatabaseName = DatabaseName(s)
  }
}
