package japgolly.webapputil.protocol.http

trait UrlEncoderApi {
  def encode(str: String): String
  def decode(str: String): String
}
