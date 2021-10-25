package japgolly.webapputil.cats.effect

import cats.effect.IO
import cats.effect.unsafe.IORuntime
import cats.effect.unsafe.implicits.global
import japgolly.webapputil.cats.effect._
import japgolly.webapputil.test.TestHttpClient
import scala.annotation.nowarn

package object test {

  type TestHttpClientIO = TestHttpClientIO.Client
  object TestHttpClientIO extends TestHttpClient.Module[IO, IO] {

    def withIORuntime(autoRespondInitially: Boolean)(implicit r: IORuntime): Client = {
      // Shadow cats.effect.unsafe.implicits.global to remove it from implicit scope
      @nowarn("cat=unused") val global = ()
      new TestHttpClient(autoRespondInitially)
    }
  }

}
