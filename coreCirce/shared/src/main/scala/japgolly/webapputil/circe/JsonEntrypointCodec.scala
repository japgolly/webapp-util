package japgolly.webapputil.circe

import io.circe._
import io.circe.parser._
import io.circe.syntax._
import japgolly.webapputil.entrypoint.EntrypointDef.Codec

object JsonEntrypointCodec {

  def apply[A: Decoder: Encoder]: Codec[A] =
    new Codec[A] {

      override val decodeOrThrow: String => A = str =>
        decode[A](str) match {
          case Right(a) => a
          case Left(e)  => JsonUtil.errorMsg(e).throwException()
        }

      override val encode: A => String =
        _.asJson.noSpacesSortKeys
    }

}
