package japgolly.webapputil.cats.effect

import cats.effect.IO
import cats.effect.unsafe.IORuntime
import japgolly.webapputil.general.Effect

object WebappUtilEffectIO {
  trait Implicits {
    @inline final implicit def webappUtilEffectIO(implicit r: IORuntime): Effect.Async[IO] with Effect.Sync[IO] =
      new WebappUtilEffectAsyncIO with Effect.Sync[IO] {
        override def runSync[A](fa: IO[A]): A =
          fa.unsafeRunSync()
      }
  }
}

// Scala 3 doesn't like this. Complains about an illegal cycle
// class WebappUtilEffectIO()(implicit runtime: IORuntime) extends WebappUtilEffectAsyncIO with Effect.Sync[IO] {
//   override def runSync[A](fa: IO[A]): A =
//     fa.unsafeRunSync()
// }
