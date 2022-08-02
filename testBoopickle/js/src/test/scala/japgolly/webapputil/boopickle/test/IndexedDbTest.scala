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

    "basicSync" - asyncTest() {
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

    "basicAsync" - asyncTest() {
      val store = ObjectStoreDef.Async("test", KeyCodec.int, ValueCodec.double.async)
      for {
        db   <- TestIndexedDb(store)
        _    <- db.add(store)(1, 123.45)
        get1 <- db.get(store)(1)
        get2 <- db.get(store)(2)
      } yield {
        assertEq(get1, Some(123.45))
        assertEq(get2, None)
      }
    }

    "put" - asyncTest() {
      val store = ObjectStoreDef.Sync("test", KeyCodec.int, ValueCodec.int)
      for {
        db    <- TestIndexedDb(store)
        _     <- db.add(store)(1, 11)
        add2  <- db.add(store)(1, 12).attempt
        _     <- db.put(store)(2, 21)
        _     <- db.put(store)(2, 22)
        get1  <- db.get(store)(1)
        get2  <- db.get(store)(2)
      } yield {
        assertEq(get1, Some(11))
        assertEq(get2, Some(22))
        assert(add2.isLeft)
        add2
      }
    }

    "closeOnUpgrade" - asyncTest() {
      val name = TestIndexedDb.freshDbName()
      val c = TestIndexedDb.unusedOpenCallbacks
      val store = ObjectStoreDef.Sync("test", KeyCodec.int, ValueCodec.string)

      for {
        verChg <- AsyncCallback.barrier.asAsyncCallback
        closed <- AsyncCallback.barrier.asAsyncCallback

        db1    <- idb.open(name, 1)(c.copy(
                    upgradeNeeded = _.createObjectStore(store, 1),
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

    "pickleCompressEncrypt" - asyncTest() {
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
      "ro+ro" - { (ro >> ro): Txn[RO, Int] }
      "ro+rw" - { (ro >> rw): Txn[RW, Int] }
    }

    "cas" - asyncTest() {
      val ids: Vector[Int] = {
        val quantity = 100
        val longest = 200
        val step = longest.toDouble / quantity
        Gen.shuffle((1 to quantity).iterator.map(_ * step).map(_.toInt).toVector).sample()
      }

      val store = ObjectStoreDef.Async("test", KeyCodec.int, ValueCodec.string.async)
      val k = 1

      def newTask(db: Database, n: Int) = {
        val blockOnce = AsyncCallback.unit.delayMs(n).memo()
        db.atomic(store).modifyAsync(k)(s => blockOnce.map(_ => s + "," + n))
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

    "openKeyCursorRO" - asyncTest() {
      val store = ObjectStoreDef.Sync("test", KeyCodec.int.xmap(_+1000)(_-1000), ValueCodec.string)
      var results1 = List.empty[Int]
      var results2 = List.empty[Int]
      var results3 = List.empty[Int]
      var results4 = List.empty[Int]
      var results5 = List.empty[Int]
      var results6 = List.empty[Int]
      for {
        db <- TestIndexedDb(store)
        _  <- db.add(store)(456, "wow")
        _  <- db.add(store)(123, "hehe")

        _  <- db.openKeyCursorRO(store) { txn => {
                case Some(c) => c.key.map(results1 ::= _) >> c.continue
                case None    => txn.delay { results1 ::= 1 }
              }}

        _  <- db.openKeyCursorRO(store) { txn => {
                case Some(c) => c.key.map(results2 ::= _) // no `continue` here
                case None    => txn.delay { results2 ::= 2 }
              }}

        _  <- db.openKeyCursorWithRangeRO(store)(_ >= 200) { txn => {
                case Some(c) => c.key.map(results3 ::= _) >> c.continue
                case None    => txn.delay { results3 ::= 3 }
              }}

        _  <- db.openKeyCursorWithRangeRO(store)(_ <= 200) { txn => {
                case Some(c) => c.key.map(results4 ::= _) >> c.key.map(results4 ::= _) >> c.continue
                case None    => txn.delay { results4 ::= 4 }
              }}

        _  <- db.openKeyCursorWithRangeRO(store)(_ only 123) { txn => {
                case Some(c) => c.key.map(results5 ::= _) >> c.continue
                case None    => txn.delay { results5 ::= 5 }
              }}

        _  <- db.openKeyCursorWithRangeRO(store)(_ > 400 < 500) { txn => {
                case Some(c) => c.key.map(results6 ::= _) >> c.continue
                case None    => txn.delay { results6 ::= 6 }
              }}

      } yield {
        assertSeqIgnoreOrder(results1)(123, 456, 1)
        assertSeqIgnoreOrder(results3)(456, 3)
        assertSeqIgnoreOrder(results4)(123, 123, 4)
        assertSeqIgnoreOrder(results5)(123, 5)
        assertSeqIgnoreOrder(results6)(456, 6)

        val result2Expect = List(List(123), List(456))
        assert(result2Expect contains results2)
      }
    }

    "openKeyCursorRW" - asyncTest() {
      val store = ObjectStoreDef.Sync("test", KeyCodec.int.xmap(_+1000)(_-1000), ValueCodec.string)
      // var results = List.empty[Int]
      // var results2 = List.empty[Int]
      // var results3 = List.empty[Int]
      // var results4 = List.empty[Int]
      // var results5 = List.empty[Int]
      // var results6 = List.empty[Int]
      for {

        barrier <- AsyncCallback.barrier.asAsyncCallback

        db <- TestIndexedDb(store)

        _  <- db.transactionRW(store) { txn =>
                txn.objectStore(store).flatMap { s =>
                  txn.traverseIterable_((100 to 400).by(50).toList) { n =>
                    s.add(n, n.toString)
                  }
                }
              }

        _  <- db.openKeyCursorRW(store) { txn => {
                case Some(c) =>
                  println("xxxxxxxx"*5)
                  for {
                    k <- c.key
                    _ <- c.delete.when_((k % 100) == 50)
                    _ <- c.continue
                  } yield ()
                case None =>
                  println("yyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyy")
                  txn.eval(barrier.complete)
              }}

        _ <- barrier.await

        results <- db.getAllKeys(store)

      } yield {
        assertSeqIgnoreOrder(results)(100, 200, 300, 400)
      }
    }

  }
}
