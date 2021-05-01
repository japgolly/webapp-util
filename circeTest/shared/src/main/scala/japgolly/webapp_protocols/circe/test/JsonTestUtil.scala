package japgolly.webapp_protocols.circe.test

import io.circe._
import io.circe.parser._
import io.circe.syntax._
import japgolly.microlibs.testutil.TestUtil._
import japgolly.webapp_protocols.circe._
import nyaya.gen.Gen
import scalaz.Equal
import scalaz.std.either._
import sourcecode.Line

object JsonTestUtil extends JsonTestUtil {
  final case class JsonPropTestQty(value: Int) extends AnyVal

  object JsonPropTestQty {
    implicit val default: JsonPropTestQty =
      apply(50)
  }

  class JsonTestUtilExtString(private val self: String) extends AnyVal {

    def jsonParseOrThrow: Json =
      parse(self) match {
        case Right(j) => j
        case Left(e)  => throw new RuntimeException(JsonUtil.errorMsg(e))
      }

    def jsonDecodeOrThrow[A: Decoder]: A =
      decode[A](self) match {
        case Right(a) => a
        case Left(e)  => throw new RuntimeException(JsonUtil.errorMsg(e))
      }
  }
}

trait JsonTestUtil extends JsonUtil.UnivEqInstances {
  import JsonTestUtil._

  implicit def JsonTestUtilExtString(self: String): JsonTestUtilExtString =
    new JsonTestUtilExtString(self)

  def assertJsonDecodeResult[A: Decoder: Equal](json: Json, expect: Decoder.Result[A])(implicit l: Line): Unit =
    assertEq(json.noSpacesSortKeys.take(180), json.as[A], expect)

  def assertJsonDecode[A: Decoder: Equal](json: String, expect: A)(implicit l: Line): Unit =
    assertJsonDecode(json.jsonParseOrThrow, expect)

  def assertJsonDecode[A: Decoder: Equal](json: Json, expect: A)(implicit l: Line): Unit =
    assertJsonDecodeResult(json, Right(expect))

  def assertJsonDecodeAll[A: Decoder: Equal](json: Seq[String], expect: Seq[A])(implicit l: Line): Unit =
    assertSeq(json.map(decode[A](_)), expect.map(Right(_)))

  def assertJsonRoundTrip[A: Decoder: Encoder: Equal](a: A, as: A*)(implicit l: Line): Unit =
    if (as.isEmpty)
      assertJsonDecode(a.asJson, a)
    else
      assertJsonRoundTrips(a +: as)

  def assertJsonRoundTrips[A: Decoder: Encoder: Equal](as: IterableOnce[A])(implicit l: Line): Unit = {
    var i = 0
    val all: Iterable[A] =
      as match {
        case x: Iterable[A] => x
        case _              => as.iterator.toList
      }
    val size = all.size
    for (a <- all) {
      i += 1
      val json = a.asJson
      assertEq(s"[$i/$size]", json.as[A], Right(a))
    }
  }

  def propTestJsonRoundTrip[A: Decoder: Encoder: Equal](g: Gen[A])(implicit l: Line, s: JsonPropTestQty): Unit =
    propTestJsonRoundTrip(g, s.value)

  def propTestJsonRoundTrip[A: Decoder: Encoder: Equal](g: Gen[A], testQty: Int)(implicit l: Line): Unit =
    assertJsonRoundTrips[A](g.samples().take(testQty))

  def jsonDecoderTester[A: Equal](d: Decoder[A]): JsonDecoderTest[A] =
    new JsonDecoderTest()(d, implicitly)
}

// =====================================================================================================================

final class JsonDecoderTest[A: Decoder: Equal] {
  import JsonTestUtil._

  def decodeOrThrow(s: String): A =
    s.jsonDecodeOrThrow[A]

  def assertDecodeResult(json: Json, expect: Decoder.Result[A])(implicit l: Line): Unit =
    assertJsonDecodeResult(json, expect)

  def assertDecode(json: String, expect: A)(implicit l: Line): Unit =
    assertJsonDecode(json, expect)

  def assertDecode(json: Json, expect: A)(implicit l: Line): Unit =
    assertJsonDecode(json, expect)
}
