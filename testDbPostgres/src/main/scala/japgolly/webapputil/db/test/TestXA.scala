package japgolly.webapputil.db.test

import doobie._
import japgolly.webapputil.db.XA
import japgolly.webapputil.db.test.TestDbHelpers._

class TestXA(xa: XA, lazyTables: () => Set[DbTable]) extends XA(xa.transactor) with TestDbHelpers {

  override def tables: Set[DbTable] =
    lazyTables()

  override def ![A](query: ConnectionIO[A]): A =
    this(query).unsafeRunSync()
}
