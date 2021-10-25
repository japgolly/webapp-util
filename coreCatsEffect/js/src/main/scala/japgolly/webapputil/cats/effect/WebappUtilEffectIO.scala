package japgolly.webapputil.cats.effect

import cats.effect.IO
import japgolly.webapputil.general.Effect

object WebappUtilEffectIO extends WebappUtilEffectAsyncIO {

  trait Implicits {
    @inline final implicit def webappUtilEffectIO: Effect.Async[IO] =
      WebappUtilEffectIO
  }
}
