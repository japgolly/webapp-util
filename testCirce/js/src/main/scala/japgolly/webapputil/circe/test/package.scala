package japgolly.webapputil.circe

import japgolly.webapputil.circe.JsonCodec
import japgolly.webapputil.test.TestAjaxClient

package object test {

  object TestJsonAjaxClient extends TestAjaxClient.Module {
    override type Codec[A] = JsonCodec[A]
  }

  type TestJsonAjaxClient = TestAjaxClient[JsonCodec]
}
