package japgolly.webapputil.cats.effect

import cats.effect.IO
import cats.effect.unsafe.IORuntime
import cats.effect.unsafe.implicits.global
import japgolly.webapputil.test.TestHttpClient

package object test {

  type TestHttpClientIO = TestHttpClientIO.Client

  object TestHttpClientIO extends TestHttpClient.Module[IO, IO] {

    def withIORuntime(autoRespondInitially: Boolean)(implicit r: IORuntime): Client = {
      val e = webappUtilEffectIO(r)
      new TestHttpClient(autoRespondInitially)(e, e)
    }
  }

}
