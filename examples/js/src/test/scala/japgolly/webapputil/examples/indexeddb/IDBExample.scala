package japgolly.webapputil.examples.indexeddb

import japgolly.scalajs.react.AsyncCallback
import japgolly.webapputil.binary._
import japgolly.webapputil.boopickle._
import japgolly.webapputil.indexeddb.IndexedDb
import java.util.UUID

object IDBExample {

  // Firstly, let's setup our dependencies.

  // 1) We need an instance of `window.indexeddb`
  val idb = IndexedDb.global()
    .getOrElse(throw new RuntimeException("indexeddb is not available"))

  // 2) We need an instance of `window.crypto`
  val encryptionEngine = EncryptionEngine.global
    .getOrElse(throw new RuntimeException("crypto is not available"))

  // 3) We need an instance of JS library `Pako` for compression
  implicit def pako: Pako = Pako.global

  // 4) We need an encryption key
  val encKey = BinaryData.fromStringAsUtf8("!" * 32)

  // Next, let's create some sample data
  val bobId  = PersonId(UUID.fromString("174b625b-9057-4d64-a92e-dee2fad89d27"))
  val bob    = Person(bobId, "Bob Loblaw", 100)

  // Now, we arrive at our examples:
  def examples: AsyncCallback[Unit] =
    for {
      enc  <- encryptionEngine(encKey) // initialise our encryption
      s     = IDBExampleStores(enc)    // initialise our stores
      db   <- s.open(idb)              // open and initialise the DB

      // ===============================================================================
      // Example 1: Simple usage
      //
      // All encoding, data compression, and encryption is handled automatically via
      // the `people` store.

      _    <- db.put(s.people)(bob.id, bob) // save a Person instance
      bob2 <- db.get(s.people)(bob.id)      // load a Person instance
      _     = assert(bob2 == Some(bob))

      // ===============================================================================
      // Example 2: Atomic modification
      //
      // In the above example, both loading and saving occur in separate transactions.
      // If one were to attempt to modify a stored Person via a get and then a set,
      // it would similarly result in two separate transactions, which would in turn
      // allow bugs if another instance of your app (eg. another browser tab or a web
      // worker) changes the database between the two transactions.
      //
      // DSL exists for working within a transaction however, due to constraints in
      // IndexedDB itself, no arbitrary async processing can occur mid-transaction.
      // We're using compression and encryption in this demo, both of which are async
      // and by necessity, must be performed outside of an IndexedDB transaction.
      // In our example above, a transaction is opened and closed automatically on both
      //  db.put() and db.get(). Thus modification via get and put wouldn't be atomic.
      //
      // Here we'll use db.atomic() to modify a person atomically, despite the necessity
      // for multiple IDB transactions. Under-the-hood, db.atomic() will call
      // db.compareAndSet() which is able to detect when external changes to the DB
      // occur, automatically retry, and only make changes when we've detected it's
      // safe to do so, and we know they're correct.

      _ <- db.atomic(s.people).modify(bob.id)(x => x.copy(age = x.age + 1))

      // ===============================================================================
      // Example 3: Transaction
      //
      // This example locks two stores into a single transaction. It atomically moves
      // pending points into Bob's earned points.

      // The RW here means "Read/Write"
      _ <- db.transactionRW(s.pointsEarned, s.pointsPending) { txn =>
        for {
          pending <- txn.objectStore(s.pointsPending)
          earned  <- txn.objectStore(s.pointsEarned)
          m       <- earned .get(bob.id).map(_ getOrElse 0)
          n       <- pending.get(bob.id).map(_ getOrElse 0)
          _       <- earned .put(bob.id, m + n)
          _       <- pending.put(bob.id, 0)
        } yield ()
      }

    } yield ()
}
