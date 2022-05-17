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

  // Next, let's create same sample data to use
  val bobId  = PersonId(UUID.fromString("174b625b-9057-4d64-a92e-dee2fad89d27"))
  val bob    = Person(bobId, "Bob Loblaw", 100)

  // And now we arrive at our examples:
  def examples: AsyncCallback[Unit] =
    for {
      enc  <- encryptionEngine(encKey) // initialise our encryption
      p     = IDBExampleProtocols(enc) // initialise our protocols
      db   <- p.open(idb)              // open and initialise the DB

      // ===============================================================================
      // Example 1: Simple usage
      //
      // All encoding, data compression, and encryption is handled automatically via
      // the `people` store.

      _    <- db.put(p.people)(bob.id, bob) // save a Person instance
      bob2 <- db.get(p.people)(bob.id)      // load a Person instance
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
      // DSL exists for working within a transaction, however due to IndexedDB
      // constraints no arbitrary async processing can occur mid-transaction.
      // We're using compression and encryption in this demo, both of which are async
      // and by necessity, must be performed outside of an IndexedDB transaction.
      //
      // Here we'll use db.atomic, which uses db.compareAndSet under-the-hood to
      // perform our operation atomically, despite its cross-transactional nature.

      _ <- db.atomic(p.people).modify(bob.id)(x => x.copy(age = x.age + 1))

      // ===============================================================================
      // Example 3: Transaction
      //
      // This example locks two stores into a single transaction. It atomically moves
      // pending points into Bob's earned points.

      _ <- db.transactionRW(p.pointsEarned, p.pointsPending) { txn =>
        for {
          pointsPending <- txn.objectStore(p.pointsPending)
          pointsEarned  <- txn.objectStore(p.pointsEarned)
          m             <- pointsEarned .get(bob.id).map(_.getOrElse(0))
          n             <- pointsPending.get(bob.id).map(_.getOrElse(0))
          _             <- pointsEarned .put(bob.id, m + n)
          _             <- pointsPending.put(bob.id, 0)
        } yield ()
      }

    } yield ()
}
