package japgolly.webapputil.boopickle.test

import boopickle.DefaultBasic._
import japgolly.microlibs.testutil.TestUtil._
import japgolly.scalajs.react._
import japgolly.webapputil.binary._
import japgolly.webapputil.boopickle.SafePickler.ConstructionHelperImplicits._
import japgolly.webapputil.boopickle._
import japgolly.webapputil.indexeddb._
import japgolly.webapputil.test.node.TestNode.asyncTest
import nyaya.gen.Gen
import utest._

object IndexedDbTest extends TestSuite {

  private implicit def idb: IndexedDb = FakeIndexedDb()
  private implicit def pako: Pako = Pako.global

  private object SampleData {

    type Data = Vector[String]

    implicit val safePicklerData: SafePickler[Data] = {
      val p: Pickler[Data] = implicitly
      p.asV1(0).withMagicNumbers(0x89827590, 0x8858F858)
    }

    val data1: Data = "qwe asd dhfg dt 5er dsfg wer gfh dfghfgsdh".split(' ').toVector
    val data2: Data = "890uh 879h 6578 giu hijo 98u0".split(' ').toVector
  }

  override def tests = Tests {

    "basicSync" - asyncTest {
      val store = ObjectStoreDef.Sync("test", KeyCodec.int, ValueCodec.string)
      for {
        db    <- TestIndexedDb(store)
        _     <- db.add(store)(1, "hello")
        _     <- db.add(store)(3, "three")
        get1  <- db.get(store)(1)
        get2  <- db.get(store)(2)
        get3  <- db.get(store)(3)
        keys1 <- db.getAllKeys(store)
        vals1 <- db.getAllValues(store)
        _     <- db.delete(store)(1)
        keys2 <- db.getAllKeys(store)
        vals2 <- db.getAllValues(store)
      } yield {
        assertEq(get1, Some("hello"))
        assertEq(get2, None)
        assertEq(get3, Some("three"))
        assertSeqIgnoreOrder(keys1)(1, 3)
        assertSeqIgnoreOrder(keys2)(3)
        assertSeqIgnoreOrder(vals1)("hello", "three")
        assertSeqIgnoreOrder(vals2)("three")
      }
    }

    "basicAsync" - asyncTest {
      val store = ObjectStoreDef.Async("test", KeyCodec.int, ValueCodec.string.async)
      for {
        db   <- TestIndexedDb(store)
        _    <- db.add(store)(1, "hello")
        get1 <- db.get(store)(1)
        get2 <- db.get(store)(2)
      } yield {
        assertEq(get1, Some("hello"))
        assertEq(get2, None)
      }
    }

    "put" - asyncTest {
      val store = ObjectStoreDef.Sync("test", KeyCodec.int, ValueCodec.string)
      for {
        db    <- TestIndexedDb(store)
        _     <- db.add(store)(1, "x1")
        add2  <- db.add(store)(1, "x2").attempt
        _     <- db.put(store)(2, "y1")
        _     <- db.put(store)(2, "y2")
        get1  <- db.get(store)(1)
        get2  <- db.get(store)(2)
      } yield {
        assertEq(get1, Some("x1"))
        assertEq(get2, Some("y2"))
        assert(add2.isLeft)
        add2
      }
    }

    "closeOnUpgrade" - asyncTest {
      val name = TestIndexedDb.freshDbName()
      val c = TestIndexedDb.unusedOpenCallbacks
      val store = ObjectStoreDef.Sync("test", KeyCodec.int, ValueCodec.string)

      for {
        verChg <- AsyncCallback.barrier.asAsyncCallback
        closed <- AsyncCallback.barrier.asAsyncCallback

        db1    <- idb.open(name, 1)(c.copy(
                    upgradeNeeded = _.createObjectStore(1, store),
                    versionChange = _ => Callback.log("db1 verChg") >> verChg.complete,
                    closed        = Callback.log("db1 closing") >> closed.complete))

        _      <- db1.add(store)(1, "omg")

        db2    <- idb.open(name, 2)(c.copy(
                    upgradeNeeded = _ => Callback.log("db2 upgrading")))

        _      <- verChg.await
        _      <- closed.await
        v1     <- db1.get(store)(1).attempt
        v2     <- db2.get(store)(1)
      } yield {
        assert(v1.isLeft)
        assertEq(v2, Some("omg"))
        v1
      }
    }

    "pickleCompressEncrypt" - asyncTest {
      import SampleData._
      import TestEncryption.UnsafeTypes._
      import ValueCodec.Async.binary

      val zip3 = Compression.ViaPako(3, addHeaders = false)
      val zip9 = Compression.ViaPako(9, addHeaders = false)

      val dbName = "IndexedDbTest_stack"
      val storeName = "s"
      val kc = KeyCodec.int
      val key1 = 123
      val key2 = 321

      for {
        enc1   <- TestEncryption("a" * 32)
        enc2   <- TestEncryption("b" * 32)
        fmt13   = BinaryFormat.pickleCompressEncrypt[Data](zip3, enc1)
        fmt19   = BinaryFormat.pickleCompressEncrypt[Data](zip9, enc1)
        fmt2    = BinaryFormat.pickleCompressEncrypt[Data](zip3, enc2)
        store13 = ObjectStoreDef.Async(storeName, kc, binary.xmapBinaryFormat(fmt13))
        store19 = ObjectStoreDef.Async(storeName, kc, binary.xmapBinaryFormat(fmt19))
        store2  = ObjectStoreDef.Async(storeName, kc, binary.xmapBinaryFormat(fmt2))
        db13   <- TestIndexedDb(dbName, store13)
        db19   <- TestIndexedDb(dbName, store19)
        db2    <- TestIndexedDb(dbName, store2)
        _      <- db13.add(store13)(key1, data1)
        _      <- db19.add(store19)(key2, data2)
        p13    <- db13.get(store13)(key1)
        p19    <- db19.get(store19)(key1)
        p23    <- db13.get(store13)(key2)
        p29    <- db19.get(store19)(key2)
        px     <- db2.get(store2)(key1).attempt
      } yield {

        // tests that protocol stack works (i.e. pickle ↔ zip ↔ enc)
        // tests that compression level changes don't prevent deserialisation
        assertEq(p13, Some(data1))
        assertEq(p19, Some(data1))
        assertEq(p23, Some(data2))
        assertEq(p29, Some(data2))

        // test different encryption key
        assert(px.isLeft)
        px
      }
    }

    "txn" - {
      import TxnMode._

      val rw: Txn[RW, Int] = TxnDslRW.pure(1)
      val ro: Txn[RO, Int] = TxnDslRO.pure(1)

      "rw≠ro" - { compileError("rw: Txn[RO, Int]") }
      "ro=rw" - { ro: Txn[RW, Int] }

      "rw+rw" - { (rw >> rw): Txn[RW, Int] }
      "rw+ro" - { (rw >> ro): Txn[RW, Int] }
      "ro-rw" - { compileError("ro >> rw") }
      "ro+ro" - { (ro >> ro): Txn[RO, Int] }
    }

    "cas" - asyncTest {

      val ids: Vector[Int] = {
        val quantity = 100
        val longest = 200
        val step = longest.toDouble / quantity
        Gen.shuffle((1 to quantity).iterator.map(_ * step).map(_.toInt).toVector).sample()
      }

      val store = ObjectStoreDef.Async("test", KeyCodec.int, ValueCodec.string.async)
      val k = 1

      def newTask(db: IndexedDb.Database, n: Int) = {
        val blockOnce = AsyncCallback.unit.delayMs(n).memo()
        db.modifyAsync(store)(k)(s => blockOnce.map(_ => s + "," + n))
      }

      for {
        db   <- TestIndexedDb(store)
        _    <- db.add(store)(k, "")
        _    <- AsyncCallback.traverse(ids)(newTask(db, _))
        ov   <- db.get(store)(k)
      } yield {
        val v = ov.get.drop(1)
        val results = v.split(',').toVector
        assertSeqIgnoreOrder(results, ids.map(_.toString))
        v
      }
    }
  }
}
