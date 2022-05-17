package japgolly.webapputil.examples.indexeddb

import boopickle.DefaultBasic._
import japgolly.scalajs.react.{AsyncCallback, Callback}
import japgolly.webapputil.binary._
import japgolly.webapputil.boopickle._
import japgolly.webapputil.indexeddb._
import java.util.UUID

// Let's say these are the data types we want to store in IndexedDb...
final case class PersonId(value: UUID)
final case class Person(id: PersonId, name: String, age: Int)

// Our protocols API.
//
// This is all that the rest of our example app will see.
trait IndexedDbProtocols {
  import IndexedDbProtocols._

  // Opens a connection to the IndexedDB database
  final def open(idb: IndexedDb): AsyncCallback[IndexedDb.Database] =
    idb.open(dbName, version)(IndexedDb.OpenCallbacks(onUpgradeNeeded))

  // Initialises or upgrades the IndexedDB database
  protected def onUpgradeNeeded(c: IndexedDb.VersionChange): Callback

  // Our IndexedDB object-store for storing our people data.
  // This is an async store because we'll compress and encrypt data before storing it;
  // compression and encryption are async operations so we define an async object-store.
  val people: ObjectStoreDef.Async[PersonId, Person]

  // We'll also create two seprate stores to later demonstrate how to use transactions.
  val pointsEarned : ObjectStoreDef.Sync[PersonId, Int]
  val pointsPending: ObjectStoreDef.Sync[PersonId, Int]
}

object IndexedDbProtocols {

  // The name of our IndexedDB database
  val dbName = IndexedDb.DatabaseName("demo")

  // The version of our combined protocols for this IndexedDB database
  val version = 1

  // Here we'll define how to covert from our data types to IndexedDB values and back.
  // We'll just define the binary formats, compression and encryption come later.
  private[IndexedDbProtocols] object Picklers {
    import SafePickler.ConstructionHelperImplicits._

    // This is a binary codec using the Boopickle library
    private implicit def picklerPersonId: Pickler[PersonId] =
      transformPickler(PersonId.apply)(_.value)

    // This is a binary codec using the Boopickle library
    private implicit def picklerPerson: Pickler[Person] =
      new Pickler[Person] {
        override def pickle(a: Person)(implicit state: PickleState): Unit = {
          state.pickle(a.id)
          state.pickle(a.name)
          state.pickle(a.age)
        }
        override def unpickle(implicit state: UnpickleState): Person = {
          val id   = state.unpickle[PersonId]
          val name = state.unpickle[String]
          val age  = state.unpickle[Int]
          Person(id, name, age)
        }
      }

    // Where `Pickler` comes from Boopickle, `SafePickler` is defined here in
    // webapp-util and provides some additional features.
    implicit def safePicklerPerson: SafePickler[Person] =
      picklerPerson
        .asV1(0) // this is v1.0 of our data format
        .withMagicNumbers(0x8CF0655B, 0x5A8218EB) // add some header/footer values for
                                                  // a bit of extra integrity.
  }

  // This is how we create an instance.
  //
  // We require two things to create an instance,
  //
  //   1) A means of binary encryption and decryption.
  //      The encryption key isn't provided directly, it will be in the Encryption
  //      instance.
  //
  //   2) An instance of Pako, a JS zlib/compression library.
  //      We could've asked for a Compression instance instead but in this example,
  //      we'll opt to configure the compression here in a static manner.
  //
  def apply(encryption: Encryption)(implicit pako: Pako): IndexedDbProtocols =
    new IndexedDbProtocols {
      import Picklers._

      // Here we configure our compression preferences:
      //   - maximum compression (i.e. set compression level to 9)
      //   - opt-out of using zlib headers
      private val compression: Compression =
        Compression.ViaPako.maxWithoutHeaders

      // Convert from PersonId to raw JS IndexedDB keys
      private val keyCodec: KeyCodec[PersonId] =
        KeyCodec.uuid.xmap(PersonId.apply)(_.value)

      // Here we define our ObjectStore for storing our collection of people.
      // This ties everything together.
      override val people: ObjectStoreDef.Async[PersonId, Person] = {

        // Our all-things-considered binary format for storing Person instances
        def valueFormat: BinaryFormat[Person] =
          // Declare that we want to support binary format evolution.
          // Eg. in the future if we were to add a new field to Person, we'd need a new
          // v1.1 format, but we'd still need to support deserialising data stored with
          // the v1.0 format.
          BinaryFormat.versioned(
            // v1.0: Use implicit SafePickler[Person] then compress & encrypt the binary
            BinaryFormat.pickleCompressEncrypt[Person](compression, encryption),
            // Our hypothetical future v1.1 protocol would be here
            // Our hypothetical future v1.2 protocol would be here
            // etc
          )

        // Convert from Person to raw JS IndexedDB values
        def valueCodec: ValueCodec.Async[Person] =
          ValueCodec.Async.binary(valueFormat)

        // Finally we create the store definition itself
        ObjectStoreDef.Async("people", keyCodec, valueCodec)
      }

      override val pointsEarned: ObjectStoreDef.Sync[PersonId, Int] =
        ObjectStoreDef.Sync("pointsEarned", keyCodec, ValueCodec.int)

      override val pointsPending: ObjectStoreDef.Sync[PersonId, Int] =
        ObjectStoreDef.Sync("pointsPending", keyCodec, ValueCodec.int)

      override protected def onUpgradeNeeded(c: IndexedDb.VersionChange): Callback =
        // In this example it creates our people store when the DB is first created.
        //
        // This is standard logic for working with IndexedDb.
        // Please google "IndexedDb upgradeneeded" for more detail.
        Callback.runAll(
          c.createObjectStore(people,        createdInDbVer = 1),
          c.createObjectStore(pointsEarned,  createdInDbVer = 1),
          c.createObjectStore(pointsPending, createdInDbVer = 1),
        )
    }
}
