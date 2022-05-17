package japgolly.webapputil.examples.indexeddb

import japgolly.webapputil.binary._
import japgolly.webapputil.boopickle.test._
import japgolly.webapputil.indexeddb._
import japgolly.webapputil.test.node.TestNode.asyncTest
import java.util.UUID
import utest._

object IDBExampleTest extends TestSuite {

  private implicit def idb: IndexedDb = FakeIndexedDb()
  private implicit def pako: Pako = Pako.global

  private val encKey = BinaryData.fromStringAsUtf8("?" * 32)
  private val stores = TestEncryption(encKey).map(IDBExampleStores(_))
  private val bob    = Person(PersonId(UUID.randomUUID()), "Bob Loblaw", 100)

  override def tests = Tests {

    "saveAndReload" - asyncTest() {
      for {
        s    <- stores
        db   <- TestIndexedDb(s.people)
        _    <- db.put(s.people)(bob.id, bob) // save a Person instance
        bob2 <- db.get(s.people)(bob.id)      // load a Person instance
      } yield {
        assert(bob2 == Some(bob))
      }
    }

  }
}
