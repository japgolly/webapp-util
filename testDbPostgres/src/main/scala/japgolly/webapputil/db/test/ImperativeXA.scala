package japgolly.webapputil.db.test

import cats.effect.Resource
import doobie._
import japgolly.webapputil.db._
import japgolly.webapputil.db.test.TestDbHelpers._

final class ImperativeXA(val xa: XA, realDb: Db, lazyTables: () => Set[DbTable]) extends TestXA(xa, lazyTables) {

  lazy val db: Db =
    Db(
      realDb.config,
      realDb.dataSource, // hmmm...
      Resource.pure(xa),
      realDb.migration)

  override def ![A](c: ConnectionIO[A]): A =
    xa(c).unsafeRun()
}
