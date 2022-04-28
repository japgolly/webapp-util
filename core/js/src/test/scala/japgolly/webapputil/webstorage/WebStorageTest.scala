package japgolly.webapputil.webstorage

import japgolly.microlibs.testutil.TestUtil._
import japgolly.webapputil.binary._
import japgolly.webapputil.webstorage.AbstractWebStorage.Key
import utest._

object WebStorageTest extends TestSuite {

  private implicit val encoder: BinaryString.Encoder =
    BinaryString.Base32768.global

  override def tests = Tests {

    "binary" - {
      val key = WebStorageKey(Key("omg"), ValueCodec.binary)

      implicit val ws = AbstractWebStorage.inMemory()

      assertEq(key.get.runNow(), None)

      val bin1 = BinaryData.fromHex("9876543210abcdef")
      key.set(bin1).runNow()
      assertEq(key.get.runNow(), Some(bin1))

      val bin2 = BinaryData.fromHex("7418529630")
      key.set(bin2).runNow()
      assertEq(key.get.runNow(), Some(bin2))
    }

  }
}
