package japgolly.webapputil.examples.ajax

import io.circe.{Decoder, Encoder}
import japgolly.webapputil.ajax.AjaxProtocol
import japgolly.webapputil.circe.JsonCodec
import japgolly.webapputil.general.{Protocol, Url}

object AjaxExampleShared {

  // This is an example AJAX endpoint definition
  object AddInts {

    val url = Url.Relative("/add-ints")

    case class Request(m: Int, n: Int)

    type Response = Long

    // Here we define the protocol between client and server.
    // Specifically, it's the URL, the request and response types, plus the codecs for serialisation & deserialisation.
    val protocol: AjaxProtocol.Simple[JsonCodec, Request, Response] = {

      implicit val decoderRequest: Decoder[Request] =
        Decoder.forProduct2("m", "n")(Request.apply)

      implicit val encoderRequest: Encoder[Request] =
        Encoder.forProduct2("m", "n")(a => (a.m, a.n))

      val requestProtocol: Protocol.Of[JsonCodec, Request] =
        Protocol(JsonCodec.summon[Request]) // ties decoderRequest and encoderRequest together

      val responseProtocol: Protocol.Of[JsonCodec, Response] =
        Protocol(JsonCodec.summon[Response]) // uses the circe's default implicits for Long <=> Json

      // "Simple" here means that the responseProtocol is static (as opposed to being dependently-typed on the request).
      AjaxProtocol.Simple(url, requestProtocol, responseProtocol)
    }

    // This is here just so that it's easily available from the example JS tests.
    //
    // In a real-project you'd share as much logic with the JS tests as possible, abstracting away things like DB
    // access. To do things properly-properly, you'd also use sbt modules to ensure this is only shared with the test JS
    // and not the main JS (so that it becomes impossible for another team-member to accidently use the logic directly
    // from JS and avoid the AJAX call).
    //
    def logic(req: Request): Response =
      req.m.toLong + req.n.toLong
  }
}
