package japgolly.webapputil.protocol.http

import scala.scalajs.js.URIUtils

object UrlEncoder extends UrlEncoderApi {

  override def encode(s: String): String =
    URIUtils.encodeURIComponent(s)
      .replace("%20", "+") // https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/encodeURIComponent#description

  override def decode(s: String): String =
    URIUtils.decodeURIComponent(
      s.replace('+', ' ') // https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/decodeURIComponent#decoding_query_parameters_from_a_url
    )
}
