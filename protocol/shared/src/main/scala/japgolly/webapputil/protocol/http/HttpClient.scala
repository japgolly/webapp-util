package japgolly.webapputil.protocol.http

import japgolly.univeq.UnivEq
import japgolly.webapputil.protocol.general.LazyVal
import scala.jdk.CollectionConverters._

object HttpClient { outer =>

  trait LibraryTypes {
    final type Body = outer.Body
    final val  Body = outer.Body

    final type Headers = outer.Headers
    final val  Headers = outer.Headers

    final type Method = outer.Method
    final val  Method = outer.Method

    final type Request = outer.Request
    final val  Request = outer.Request

    final type Response = outer.Response
    final val  Response = outer.Response

    final type ResponseBody = outer.ResponseBody
    final val  ResponseBody = outer.ResponseBody

    final type Status = outer.Status
    final val  Status = outer.Status

    final type StringMap = outer.StringMap
    final val  StringMap = outer.StringMap

    final type UriParams = outer.UriParams
    final val  UriParams = outer.UriParams
  }

  type WithEffect[F[_]] = Request => F[Response]

  trait HttpClientType[F[_]] {
    final type HttpClient = WithEffect[F]
  }

  trait Module[F[_]] extends HttpClientType[F] with LibraryTypes {
    val HttpClient: HttpClient
  }

  object Module {
    def apply[F[_]](f: Request => F[Response]): Module[F] =
      new Module[F] {
        override val HttpClient = f
      }
  }

  // ===================================================================================================================

  final case class StringMap(asVector: Vector[(String, String)]) extends AnyVal {
    def isEmpty  = asVector.isEmpty
    def nonEmpty = asVector.nonEmpty

    def add(key: String, value: String): StringMap =
      new StringMap(asVector :+ ((key, value)))

    def delete(key: String): StringMap =
      new StringMap(asVector.filter(_._1 != key))

    def get(key: String): Vector[String] =
      asVector.iterator.filter(_._1 == key).map(_._2).toVector
  }

  object StringMap extends StringMapLikeHelpers[StringMap] {
    override def fromSeq(s: Seq[(String, String)]): StringMap =
      new StringMap(s.toVector)
  }

  trait StringMapLikeHelpers[+A] {
    def fromSeq(s: Seq[(String, String)]): A

    final def fromMap(m: Map[String, String]): A =
      fromSeq(m.toVector)

    final def apply(kvs: (String, String)*): A =
      fromSeq(kvs.toVector)

    final val empty: A =
      fromSeq(Vector.empty)

    final def fromJavaMultimap[C <: java.util.Collection[String]](multimap: java.util.Map[String, C]): A =
      fromSeq(
        multimap
          .entrySet()
          .stream()
          .iterator()
          .asScala
          .flatMap(e => e.getValue.iterator().asScala.map(e.getKey -> _))
          .toVector,
      )
  }

  final case class Headers(val asVector: Vector[(String, String)]) extends AnyVal {
    def isEmpty                            = asVector.isEmpty
    def nonEmpty                           = asVector.nonEmpty
    def stringMap                          = new StringMap(asVector)
    def add   (key: String, value: String) = new Headers(stringMap.add(key, value).asVector)
    def delete(key: String)                = new Headers(stringMap.delete(key).asVector)
    def get   (key: String)                = stringMap.get(key)

    def withContentType(value: String) = add("Content-Type", value)
    def withContentTypeBinary          = withContentType(Headers.ContentType.Binary)
    def withContentTypeForm            = withContentType(Headers.ContentType.Form)
    def withContentTypeJson            = withContentType(Headers.ContentType.Json)
    def withContentTypeJsonUtf8        = withContentType(Headers.ContentType.JsonUtf8)
  }

  object Headers extends StringMapLikeHelpers[Headers] {
    override def fromSeq(s: Seq[(String, String)]): Headers =
      new Headers(s.toVector)

    object ContentType {
      final val Binary   = "application/octet-stream"
      final val Json     = "application/json"
      final val JsonUtf8 = "application/json;charset=UTF-8"
      final val Form     = "application/x-www-form-urlencoded"

      def is(contentType: String): String => Boolean = {
        val len = contentType.length
        c => c.startsWith(contentType) && (
          c.length == len  // exact match
          || c(len) == ';' // prefix
        )
      }

      val isJson = is(Json)
    }
  }

  final class UriParams(val asVector: Vector[(String, String)], isNormalised: Boolean) {
    def isEmpty                            = asVector.isEmpty
    def nonEmpty                           = asVector.nonEmpty
    def stringMap                          = new StringMap(asVector)
    def add   (key: String, value: String) = UriParams(stringMap.add(key, value).asVector)
    def delete(key: String)                = UriParams(stringMap.delete(key).asVector)
    def get   (key: String)                = stringMap.get(key)

    def asString: String =
      if (isEmpty)
        ""
      else {
        val sb    = new java.lang.StringBuilder
        var first = true
        for (x <- asVector) {
          import x.{_1 => k, _2 => v}
          if (first)
            first = false
          else
            sb.append('&')
          sb.append(UrlEncoder.encode(k))
          if (v != null) {
            sb.append('=')
            sb.append(UrlEncoder.encode(v))
          }
        }
        sb.toString
      }

    def normalised: UriParams =
      if (isNormalised)
        this
      else
        reNormalised

    private lazy val reNormalised: UriParams = {
      // According to the Scala doc, this is a stable sort
      val result = asVector.sortBy(_._1)
      new UriParams(result, isNormalised = true)
    }

    override def toString =
      asVector
        .iterator
        .map(x => s"${x._1} -> ${x._2}")
        .mkString("UriParams(", ", ", ")")

    override def hashCode =
      normalised.asVector.hashCode

    override def equals(that: Any) =
      that match {
        case t: UriParams => normalised.asVector == t.normalised.asVector
        case _            => false
      }
  }

  object UriParams extends StringMapLikeHelpers[UriParams] {

    def apply(asVector: Vector[(String, String)]): UriParams =
      new UriParams(asVector, isNormalised = false)

    override def fromSeq(s: Seq[(String, String)]): UriParams =
      apply(s.toVector)

    def parse(body: String): UriParams =
      if ((body == null) || body.isEmpty)
        empty
      else {
        // TODO: Could be more optimised
        var ps    = empty
        val frags = body.split('&')
        var frag  = null: String
        var key   = null: String
        var value = null: String
        var i     = 0
        var j     = 0
        while (i < frags.length) {
          frag = frags(i)
          j = frag.indexOf('=')
          if (j >= 0) {
            key   = UrlEncoder.decode(frag.take(j))
            value = UrlEncoder.decode(frag.drop(j + 1))
            ps    = ps.add(key, value)
          } else {
            key = UrlEncoder.decode(frag)
            ps  = ps.add(key, null)
          }
          i += 1
        }
        ps
      }
  }

  final case class Method(asString: String)
  object Method {
    val CONNECT = apply("CONNECT")
    val DELETE  = apply("DELETE")
    val GET     = apply("GET")
    val HEAD    = apply("HEAD")
    val OPTIONS = apply("OPTIONS")
    val PATCH   = apply("PATCH")
    val POST    = apply("POST")
    val PUT     = apply("PUT")
    val TRACE   = apply("TRACE")
  }

  sealed trait Body

  object Body {

    def apply(body: String, contentType: String = null): Body =
      contentType match {
        case Headers.ContentType.Form =>
          if (body == null)
            Form.empty
          else
            Form(UriParams.parse(body))
        case _ =>
          val s = if (body == null) "" else body
          Str(s, Option(contentType))
      }

    final case class Form(params: UriParams) extends Body {
      def add   (key: String, value: String) = Form(params.add(key, value))
      def delete(key: String)                = Form(params.delete(key))
      def get   (key: String)                = params.get(key)
    }

    object Form extends StringMapLikeHelpers[Form] {
      override def fromSeq(s: Seq[(String, String)]): Form =
        new Form(UriParams(s.toVector))
    }

    final case class Str(content: String, contentType: Option[String]) extends Body {
      def isEmpty: Boolean =
        content.isEmpty && contentType.isEmpty

      def isContentTypeJson        = contentType.exists(Headers.ContentType.isJson)
      def isContentTypeJsonOrEmpty = contentType.forall(Headers.ContentType.isJson)
    }

    val empty: Str =
      Str("", None)

    val emptyLazy: LazyVal[Str] =
      LazyVal.pure(empty)
  }

  final case class Request(method   : Method,
                           uri      : String,
                           uriParams: UriParams,
                           headers  : Headers,
                           body     : Body,
                          )

  trait RequestCtors[+A] {
    def apply(method   : Method,
              uri      : String,
              uriParams: UriParams = UriParams.empty,
              headers  : Headers   = Headers.empty,
              body     : Body      = Body.empty,
             ): A

    @inline final def get(uri      : String,
                          uriParams: UriParams = UriParams.empty,
                          headers  : Headers   = Headers.empty,
                          body     : Body      = Body.empty,
                         ): A =
      apply(Method.GET, uri, uriParams, headers, body)

    @inline final def post(uri      : String,
                           uriParams: UriParams = UriParams.empty,
                           headers  : Headers   = Headers.empty,
                           body     : Body      = Body.empty,
                          ): A =
      apply(Method.POST, uri, uriParams, headers, body)
  }

  object Request extends RequestCtors[Request] {
    def apply(method   : Method,
              uri      : String,
              uriParams: UriParams = UriParams.empty,
              headers  : Headers   = Headers.empty,
              body     : Body      = Body.empty,
             ): Request =
      new Request(method, uri, uriParams, headers, body)
  }

  final case class Status(code: Int) extends AnyVal {
    def is1xx     = code >= 100 && code < 200
    def is2xx     = code >= 200 && code < 300
    def is3xx     = code >= 300 && code < 400
    def is4xx     = code >= 400 && code < 500
    def is5xx     = code >= 500 && code < 600
    def isSuccess = is2xx || (code == 304)
  }

  final case class Response(status : Status,
                            body   : LazyVal[ResponseBody] = ResponseBody.emptyLazy,
                            headers: Headers               = Headers.empty,
                           )

  type ResponseBody = Body.Str

  object ResponseBody {
    def apply(content: String, contentType: Option[String]): ResponseBody =
      Body.Str(content, contentType)

    def empty    : ResponseBody          = Body.empty
    def emptyLazy: LazyVal[ResponseBody] = Body.emptyLazy
  }


  implicit def univeqBody     : UnivEq[Body     ] = UnivEq.derive
  implicit def univeqBodyStr  : UnivEq[Body.Str ] = UnivEq.derive
  implicit def univeqHeaders  : UnivEq[Headers  ] = UnivEq.derive
  implicit def univeqMethod   : UnivEq[Method   ] = UnivEq.force
  implicit def univeqRequest  : UnivEq[Request  ] = UnivEq.derive
  implicit def univeqResponse : UnivEq[Response ] = UnivEq.derive
  implicit def univeqStatus   : UnivEq[Status   ] = UnivEq.derive
  implicit def univeqStringMap: UnivEq[StringMap] = UnivEq.derive
  implicit def univeqUriParams: UnivEq[UriParams] = UnivEq.force

}
