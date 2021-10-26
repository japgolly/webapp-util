package japgolly.webapputil.cats.effect

import cats.effect.IO
import cats.effect.unsafe.IORuntime

class WebappUtilIOExt[A](private val self: IO[A]) extends AnyVal {

  def toJavaRunnable(implicit r: IORuntime): Runnable =
    () => {
      self.unsafeRunSync()(r)
      ()
    }
}
