package japgolly.webapputil.circe.test

import cats.Eq
import cats.instances.either._
import io.circe._
import io.circe.parser._
import io.circe.syntax._
import japgolly.microlibs.testutil.TestUtil._
import japgolly.webapputil.circe._
import nyaya.gen.Gen
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
        case Left(e)  => JsonUtil.errorMsg(e).throwException()
      }

    def jsonDecodeOrThrow[A: Decoder]: A =
      decode[A](self) match {
        case Right(a) => a
        case Left(e)  => JsonUtil.errorMsg(e).throwException()
      }
  }
}

trait JsonTestUtil extends JsonUtil.UnivEqInstances {
  import JsonTestUtil._

  implicit def JsonTestUtilExtString(self: String): JsonTestUtilExtString =
    new JsonTestUtilExtString(self)

  def assertJsonDecodeResult[A: Decoder: Eq](json: Json, expect: Decoder.Result[A])(implicit l: Line): Unit =
    assertEq(json.noSpacesSortKeys.take(180), json.as[A], expect)

  def assertJsonDecode[A: Decoder: Eq](json: String, expect: A)(implicit l: Line): Unit =
    assertJsonDecode(json.jsonParseOrThrow, expect)

  def assertJsonDecode[A: Decoder: Eq](json: Json, expect: A)(implicit l: Line): Unit =
    assertJsonDecodeResult(json, Right(expect))

  def assertJsonDecodeAll[A: Decoder: Eq](json: Seq[String], expect: Seq[A])(implicit l: Line): Unit =
    assertSeq(json.map(decode[A](_)), expect.map(Right(_)))

  def assertJsonEncode[A: Encoder: Eq](a: A, expect: String)(implicit l: Line): Unit =
    assertJsonEncode(a, expect.jsonParseOrThrow)

  def assertJsonEncode[A: Encoder: Eq](a: A, expect: Json)(implicit l: Line): Unit =
    assertEq("" + a, a.asJson, expect)

  def assertJsonEncodeDecode[A: Decoder: Encoder: Eq](a: A, json: String)(implicit l: Line): Unit =
    assertJsonEncodeDecode(a, json.jsonParseOrThrow)

  def assertJsonEncodeDecode[A: Decoder: Encoder: Eq](a: A, json: Json)(implicit l: Line): Unit = {
    assertJsonEncode(a, json)
    assertJsonDecode(json, a)
  }

  def assertJsonRoundTrip[A: Decoder: Encoder: Eq](a: A, as: A*)(implicit l: Line): Unit =
    if (as.isEmpty)
      assertJsonDecode(a.asJson, a)
    else
      assertJsonRoundTrips(a +: as)

  def assertJsonRoundTrips[A: Decoder: Encoder: Eq](as: IterableOnce[A])(implicit l: Line): Unit = {
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

  def propTestJsonRoundTrip[A: Decoder: Encoder: Eq](g: Gen[A])(implicit l: Line, s: JsonPropTestQty): Unit =
    propTestJsonRoundTrip(g, s.value)

  def propTestJsonRoundTrip[A: Decoder: Encoder: Eq](g: Gen[A], testQty: Int)(implicit l: Line): Unit =
    assertJsonRoundTrips[A](g.samples().take(testQty))

  def jsonDecoderTester[A: Eq](d: Decoder[A]): JsonDecoderTest[A] =
    new JsonDecoderTest()(d, implicitly)
}

// =====================================================================================================================

final class JsonDecoderTest[A: Decoder: Eq] {
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
