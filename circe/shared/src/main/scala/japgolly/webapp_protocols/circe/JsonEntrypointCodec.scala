package japgolly.webapp_protocols.circe

import japgolly.webapp_protocols.core.entrypoint.EntrypointDef.Codec
import io.circe._
import io.circe.parser._
import io.circe.syntax._

object JsonEntrypointCodec {

  def apply[A: Decoder: Encoder]: Codec[A] =
    new Codec[A] {

      override val decodeOrThrow: String => A = str =>
        decode[A](str) match {
          case Right(a) => a
          case Left(e)  => throw new RuntimeException(JsonUtil.errorMsg(e))
        }

      override val encode: A => String =
        _.asJson.noSpacesSortKeys
    }

}
