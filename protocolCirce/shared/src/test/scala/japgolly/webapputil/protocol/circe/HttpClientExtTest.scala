package japgolly.webapputil.protocol.circe

import cats.Eq
import japgolly.microlibs.testutil.TestUtil._
import utest._
import japgolly.webapputil.protocol.http.HttpClient._

object HttpClientExtTest extends TestSuite {

  private implicit def eqHttpJsonParseFailure: Eq[HttpJsonParseFailure] =
    Eq.fromUniversalEquals

  override def tests = Tests {

    "body" - {

      "json" - {
        val a = Body.json("he")
        assertEq(a, Body.Str("\"he\"", Some(Headers.ContentType.JsonUtf8)))
      }

      "parseJsonBody" - {
        "ok" - {
          val b: Body = Body.Str("\"he\"", Some(Headers.ContentType.JsonUtf8))
          assertEq(b.parseJsonBody[String], Right("he"))
        }

        "wrongHeader" - {
          val b: Body = Body.Str("1", Some(Headers.ContentType.Binary))
          assertMatch(b.parseJsonBody[String]) { case Left(_: HttpJsonParseFailure.NonJsonContentType) => }
        }

        "cantParse" - {
          val b: Body = Body.Str("\"he", Some(Headers.ContentType.JsonUtf8))
          assertMatch(b.parseJsonBody[String]) { case Left(_: HttpJsonParseFailure.JsonParseError) => }
        }
      }

    }
  }
}
