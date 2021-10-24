package japgolly.webapputil.http

trait UrlEncoderApi {
  def encode(str: String): String
  def decode(str: String): String
}
