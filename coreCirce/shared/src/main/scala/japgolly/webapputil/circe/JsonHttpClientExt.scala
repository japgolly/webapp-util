package japgolly.webapputil.circe

import cats.syntax.either._
import io.circe._
import io.circe.parser.parse
import io.circe.syntax._
import japgolly.webapputil.http.HttpClient._

trait JsonHttpClientExt {
  import JsonHttpClientExt._

  /** Allows `Body.json(myObject)` */
  @inline final implicit def circeHttpClientBodyObjExt(a: Body.type): BodyObjExt = new BodyObjExt(a)

  /** Allows `Body#parseJsonBody[A]` */
  @inline final implicit def circeHttpClientBodyExt(a: Body): BodyExt = new BodyExt(a)
}

object JsonHttpClientExt {

  final class BodyObjExt(private val self: Body.type) extends AnyVal {

    /** Allows `Body.json(myObject)` */
    @inline def json = BodyObjJson
  }

  object BodyObjJson {
    @inline private def contentType =
      Some(ContentType.JsonUtf8)

    def apply[A: Encoder](a: A): Body.Str =
      Body.Str(a.asJson.noSpaces, contentType)

    def sortKeys[A: Encoder](a: A): Body.Str =
      Body.Str(a.asJson.noSpacesSortKeys, contentType)

    def spaces2[A: Encoder](a: A): Body.Str =
      Body.Str(a.asJson.spaces2, contentType)

    def spaces2SortKeys[A: Encoder](a: A): Body.Str =
      Body.Str(a.asJson.spaces2SortKeys, contentType)
  }

  // ===================================================================================================================

  final class BodyExt(private val self: Body) extends AnyVal {

    def parseJsonBody[A: Decoder]: Either[HttpJsonParseFailure, A] =
      self match {
        case body: Body.Str =>
          if (!body.isContentTypeJsonOrEmpty)
            Left(HttpJsonParseFailure.NonJsonContentType(body.contentType.getOrElse("")))
          else
            for {
              json <- parse(body.content).leftMap(HttpJsonParseFailure.JsonParseError)
              a    <- json.as[A].leftMap(HttpJsonParseFailure.JsonDecodeError)
            } yield a
        case f: Body.Form =>
          Left(HttpJsonParseFailure.NonJsonContentType(f.contentType))
      }
  }
}

sealed trait HttpJsonParseFailure
object HttpJsonParseFailure {
  final case class JsonParseError    (failure: ParsingFailure)  extends HttpJsonParseFailure
  final case class JsonDecodeError   (failure: DecodingFailure) extends HttpJsonParseFailure
  final case class NonJsonContentType(contentType: String)      extends HttpJsonParseFailure
}
