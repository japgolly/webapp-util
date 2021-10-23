package japgolly.webapputil.protocol.http

import japgolly.microlibs.testutil.TestUtil._
import nyaya.gen._
import utest._

object HttpClientTest extends TestSuite {
  import HttpClient._

  val genUriParams: Gen[UriParams] = {
    val char   = Gen.chooseGen(Gen.char, Gen.alphaNumeric, Gen.chooseChar('=', "&?+ "))
    val genKey = char.string(1 to 4)
    val genStr = char.string(0 to 8)
    val genVal = Gen.chooseGen(genStr, genStr, genStr, Gen.pure[String](null))
    (genKey & genVal).list(0 to 8).map(UriParams.fromSeq)
  }

  override def tests = Tests {

    "uriParams" - {
      "spot" - {
        val src = UriParams(
          "a" -> "1",
          "c" -> "x = & + x",
          "n" -> null,
          "a" -> "2",
        )
        val str = src.asString
        assertEq(str, "a=1&c=x+%3D+%26+%2B+x&n&a=2")

        val ps2 = UriParams.parse(str)
        assertEq(ps2.asVector, src.asVector)
      }

      "roundTrip" - {
        for (src <- genUriParams.samples().take(80)) {
          val str = src.asString
          val ps2 = UriParams.parse(str)
          // println()
          // println(src.asVector)
          // println("\"" + str + "\"")
          // assertEq(str, ps2.asVector, src.asVector)
          assertSeq(str, ps2.asVector, src.asVector)
        }
      }

      "normalisation" - {
        val src = UriParams("b" -> "2", "z" -> "1", "b" -> "0", "a" -> "6")
        val tgt = UriParams("a" -> "6", "b" -> "2", "b" -> "0", "z" -> "1")
        assertEq(src.normalised.asVector, tgt.asVector)
        assertEq(src, tgt)
      }
    }
  }
}