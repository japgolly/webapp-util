package japgolly.webapputil.examples.ajax

import io.circe.{Decoder, Json}

object ExampleAjaxJvm {

  // This takes JSON and returns JSON.
  //
  // It's left as an exercise to the reader to integrate a JSON-to-JSON endpoint into your web server of choice.
  //
  def serveAddInts(requestJson: Json): Decoder.Result[Json] = {
    import ExampleAjaxShared.AddInts.{logic, protocol}

    protocol.requestProtocol.codec.decode(requestJson).map { request =>
      val response     = logic(request)
      val responseJson = protocol.responseProtocol(request).codec.encode(response)
      responseJson
    }
  }

}
