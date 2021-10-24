package japgolly.webapputil.http

import java.net.{URLDecoder, URLEncoder}
import java.nio.charset.StandardCharsets

object UrlEncoder extends UrlEncoderApi {

  private[this] val utf8 = StandardCharsets.UTF_8

  override def encode(s: String): String =
    URLEncoder.encode(s, utf8)

  override def decode(s: String): String =
    URLDecoder.decode(s, utf8)
}
