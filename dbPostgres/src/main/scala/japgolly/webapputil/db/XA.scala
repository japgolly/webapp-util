package japgolly.webapputil.db

import cats.effect.IO
import cats.~>
import doobie._

class XA(val transactor: Transactor[IO]) extends (ConnectionIO ~> IO) {

  private[this] val trans: (ConnectionIO ~> IO) =
    transactor.trans

  override def apply[A](c: ConnectionIO[A]): IO[A] =
    trans(c)
}
