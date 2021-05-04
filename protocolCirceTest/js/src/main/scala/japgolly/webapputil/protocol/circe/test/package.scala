package japgolly.webapputil.protocol.circe

import japgolly.webapputil.protocol.test.TestAjaxClient
import japgolly.webapputil.protocol.circe.JsonCodec

package object test {

  object TestJsonAjaxClient extends TestAjaxClient.Module {
    override type Codec[A] = JsonCodec[A]
  }

  type TestJsonAjaxClient = TestJsonAjaxClient.Client
}
