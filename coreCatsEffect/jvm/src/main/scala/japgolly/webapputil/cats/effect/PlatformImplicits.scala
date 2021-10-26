package japgolly.webapputil.cats.effect

import cats.effect.IO

trait PlatformImplicits  {

  @inline final implicit def webappUtilIOExt[A](io: IO[A]): WebappUtilIOExt[A] =
    new WebappUtilIOExt(io)
}
