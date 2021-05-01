package japgolly.webapp_protocols.core.test

import japgolly.webapp_protocols.core.binary.BinaryData
import japgolly.microlibs.testutil.TestUtil._
import sourcecode.Line

object BinaryTestUtil extends BinaryTestUtil

trait BinaryTestUtil {

  def assertBinaryEq(actual: BinaryData, expect: BinaryData)(implicit l: Line): Unit =
    binaryDiff(actual, expect).foreach(fail(_))

  def binaryDiff(actual    : BinaryData,
                 expect    : BinaryData,
                 descActual: String = "Actual",
                 descExpect: String = "Expect",
                 limit     : Int    = 100
                ): Option[String] =
    Option.when(actual !=* expect) {

      var failures = List.empty[String]

      if (actual.length != expect.length)
        failures ::= s"Actual length (${actual.length}) != expect ${expect.length}"

      var b1 = actual
      var b2 = expect

      var pre = ""
      var post = ""

      def tooBig() = b1.length > limit || b2.length > limit

      if (tooBig()) {
        while (tooBig() && b1.unsafeArray(0) == b2.unsafeArray(0)) {
          b1 = b1.drop(1)
          b2 = b2.drop(1)
          pre = "…"
        }

        if (tooBig()) {
          b1 = b1.take(limit)
          b2 = b2.take(limit)
          post = "…"
        }
      }

      val (s1, s2) = {
        var r1 = Console.BLACK_B
        var r2 = Console.BLACK_B
        var h1 = b1.hex
        var h2 = b2.hex
        while (h1.nonEmpty || h2.nonEmpty) {
          val b1 = h1.take(2)
          val b2 = h2.take(2)
          if (b1 ==* b2) {
            r1 += b1
            r2 += b2
          } else {
            r1 += Console.YELLOW_B + b1 + Console.BLACK_B
            r2 += Console.YELLOW_B + b2 + Console.BLACK_B
          }
          h1 = h1.drop(2)
          h2 = h2.drop(2)
        }
        (r1, r2)
      }
      failures :+=
        s"""
           |$descActual: $pre$s1$post
           |$descExpect: $pre$s2$post
           |""".stripMargin

      failures.mkString("\n")
    }

}
