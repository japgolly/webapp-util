package japgolly.webapputil.protocol.http

import japgolly.webapputil.protocol.general.{Effect, LazyVal}
import java.io.IOException
import okhttp3.{Call, Callback, FormBody, HttpUrl, MediaType, OkHttpClient => OkClient}
import scala.annotation.nowarn
import scala.util.{Failure, Success}

object OkHttp4Client {
  def apply[F[_]](okhttp: OkClient)(implicit F: Effect.Async[F]): OkHttp4Client[F] =
    new OkHttp4Client(okhttp)
}

class OkHttp4Client[F[_]](okhttp: OkClient)(implicit F: Effect.Async[F]) extends HttpClient.Module[F] {

  @nowarn("msg=outer reference in this type test")
  override val HttpClient:  HttpClient = {
    def convertRequest(req: Request): okhttp3.Request = {
      val url: HttpUrl =
        if (req.uriParams.isEmpty)
          HttpUrl.get(req.uri)
        else {
          val urlBuilder = HttpUrl.parse(req.uri).newBuilder()
          req.uriParams.asVector.foreach { case (k, v) =>
            urlBuilder.addQueryParameter(k, v)
          }
          urlBuilder.build()
        }

      val headers: okhttp3.Headers =
        req
          .headers
          .asVector
          .foldLeft(new okhttp3.Headers.Builder) { case (b, (k, v)) => b.add(k, v) }
          .build()

      val body: okhttp3.RequestBody =
        req.body match {
          case Body.Str(content, None) =>
            okhttp3.RequestBody.create(content.getBytes())

          case Body.Str(content, Some(contentType)) =>
            okhttp3.RequestBody.create(content, MediaType.get(contentType))

          case Body.Form(params) =>
            val formBody = new FormBody.Builder()
            params.asVector.foreach { case (k, v) =>
              formBody.add(k, v)
            }
            formBody.build()
        }

      new okhttp3.Request.Builder()
        .url(url)
        .headers(headers)
        .method(req.method.asString, body)
        .build()
    }

    def convertResponse(res: okhttp3.Response): Response = {
      def body = ResponseBody(
        content = res.body().string(),
        contentType = Option(res.body().contentType()).map(_.toString),
      )
      Response(
        status = Status(res.code()),
        body = LazyVal(body),
        headers = Headers.fromJavaMultimap(res.headers().toMultimap),
      )
    }

    def exec(req: okhttp3.Request): F[okhttp3.Response] =
      F.async[okhttp3.Response] { callback =>
        okhttp
          .newCall(req)
          .enqueue(new Callback {
            override def onFailure(call: Call, e: IOException): Unit         = callback(Failure(e))
            override def onResponse(call: Call, res: okhttp3.Response): Unit = callback(Success(res))
          })
      }

    req => F.map(exec(convertRequest(req)))(convertResponse)
  }
}
