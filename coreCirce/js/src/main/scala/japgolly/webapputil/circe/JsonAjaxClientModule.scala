package japgolly.webapputil.circe

import io.circe.parser.{decode => circeDecode}
import japgolly.webapputil.ajax.AjaxClient

trait JsonAjaxClientModule {

  type JsonAjaxClient = AjaxClient[JsonCodec]

  object JsonAjaxClient extends DefaultJsonAjaxClient

  trait DefaultJsonAjaxClient extends AjaxClient.Json[JsonCodec] {

    override protected def encode[A](p: JsonCodec[A], a: A): String =
      p.encoder(a).noSpacesSortKeys

    override protected def decode[A](p: JsonCodec[A], j: String): AjaxClient.Response[A] =
      circeDecode[A](j)(p.decoder) match {
        case Right(a) => AjaxClient.Response.success(a)
        case Left(e)  => AjaxClient.Response(Left(JsonUtil.errorMsg(e)))
      }
  }

}
