package japgolly.webapputil.boopickle.test

import japgolly.webapputil.indexeddb._
import japgolly.webapputil.test.node.TestNode

object FakeIndexedDb {

  private lazy val instance: IndexedDb = {
    val sbtRootDir = TestNode.envVarNeed("SBT_ROOT")
    val distDir    = sbtRootDir + "/jsBundles/dist"
    TestIndexedDb.loadLocalFakeIndexedDbBundle(s"$distDir/fake-indexeddb")
  }

  implicit def apply(): IndexedDb =
    instance
}
