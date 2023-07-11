package japgolly.webapputil.db.test

import cats.effect.{IO, Resource}
import cats.syntax.apply._
import com.typesafe.scalalogging.Logger
import doobie._
import doobie.free.{connection => C}
import doobie.implicits._
import doobie.util.Colors
import doobie.util.testing._
import doobie.util.transactor.Strategy
import izumi.reflect.Tag
import japgolly.microlibs.testutil.TestUtil._
import japgolly.webapputil.cats.effect._
import japgolly.webapputil.db._
import japgolly.webapputil.db.test.TestDbHelpers._
import japgolly.webapputil.general.ThreadUtils
import japgolly.webapputil.locks.SharedLock
import java.sql.Connection
import java.util.concurrent.atomic.AtomicReference
import javax.sql.DataSource
import scala.concurrent.ExecutionContext
import sourcecode.Line

/** Extend this to create your own:
 *
 * {{{
 *   object TestDb extends japgolly.webapputil.db.test.TestDb {
 *
 *     override protected def cfg: DbConfig =
 *       xxx
 *
 *     onlyAllowDropSchemaWhenDatabaseNameEndsWith("_test")
 *
 *   }
 * }}}
 */
abstract class TestDb {

  protected def cfg: DbConfig

  protected def addShutdownHook =
    true

  protected val logger: Logger =
    Logger(getClass)

  protected def transactionIsolation: Option[Int] =
    Some(Connection.TRANSACTION_SERIALIZABLE)

  protected def initConnection: ConnectionIO[Unit] =
    transactionIsolation match {
      case None    => C.unit
      case Some(t) => DoobieHelpers.setTransactionLevelIfNeeded(t)
    }

  /** Rollback everything */
  protected def txnStrategy =
    Strategy(initConnection *> C.setAutoCommit(false), C.rollback, C.rollback, C.unit)

  private def load(): Db = {
    logger.info("Creating Test Db...")

    val poolSize = if (cfg.poolSize == -1) 4 else cfg.poolSize
    assert(poolSize >= 1, s"DB pool size = $poolSize ?!")

    var dataSrc: DataSource = cfg.pgDataSource
    for (t <- cfg.sqlTracer)
      dataSrc = t.inject(dataSrc)

    val sync = true

    val executionContext: Resource[IO, ExecutionContext] =
      if (sync)
        Resource.pure[IO, ExecutionContext](ExecutionContexts.synchronous)
      else
        ThreadUtilsIO.threadPool("TestDb", logger)(_.withThreads(poolSize))

    val create =
      Db.generic(
        cfg         = cfg,
        dsMain      = dataSrc,
        dsMigration = dataSrc,
        connectEC   = executionContext,
        transactor  = Transactor.fromDataSource[IO](dataSrc, _).copy(strategy0 = txnStrategy),
      )

    create.unsafeRun()
  }

  private case class State(db: Db, xa: XA, shutdown: IO[Unit])

  @volatile private[this] var _state: Option[State] =
    None

  def initialised()          = _state.isDefined
  def initPending()          = !initialised()
  private[this] val initLock = new AnyRef

  def init(): Unit =
    if (initPending())
      initLock.synchronized(
        if (initPending()) {
          logger.info("Database initialising...")
          val db            = load()
          val (xa, release) = db.xa.allocated.unsafeRun()
          _state = Some(State(db, xa, release))
          if (addShutdownHook)
            ThreadUtils.runOnShutdown("TestDb")(shutdown())
          try {
            db.migration.migrate.unsafeRun()
          } catch {
            case _: Throwable =>
              dropSchema()
              db.migration.migrate.unsafeRun()
          }
          truncateAllWithoutLocking()
          logger.info("Database initialised.")
        },
      )

  def shutdown(): Unit =
    // No write-lock here because if I run only a single test in LiveTest, it doesn't shutdown which doesn't release
    // the write-lock which freezes everything. Rather than avoid that scenario, let's just not use a write-lock here
    // and avoid all similar issues. Remember that this is TestDb.shutdown() which NO ONE calls except for:
    // 1) SBT via Common.scala
    // 2) the shutdown hook registered in init() above
    if (initialised())
      _state match {
        case Some(s) =>
          logger.info("Database shutting down...")
          s.shutdown.unsafeRun()
          _state = None
          logger.info("Database shut down.")
        case None =>
          ()
      }

  private def state(): State = {
    init()
    _state.get
  }

  private def db(): Db =
    state().db

  private def xaWithoutLocking(): XA =
    state().xa

  private val xaWithoutLockingIO: IO[XA] =
    IO(xaWithoutLocking())

  private[this] val rwlock = SharedLock.ReadWrite()

  /** Called when the DB schema is about to be dropped. */
  val onDropSchemaAttempt: AtomicReference[Db => Unit] =
    new AtomicReference(_ => ())

  def onlyAllowDropSchemaWhenDatabaseNameWhen(allow: String => Boolean, errMsg: String => String): Unit =
    onDropSchemaAttempt.updateAndGet { prevHandler => db =>
      val dbName = db.databaseName
      if (!allow(dbName))
        sys.error(errMsg(dbName))
      prevHandler(db)
    }

  def onlyAllowDropSchemaWhenDatabaseNameIs(expected: String): Unit =
    onlyAllowDropSchemaWhenDatabaseNameWhen(
      _ == expected,
      name => s"You're trying to wipe $name. Only $expected is allowed to be dropped.")

  def onlyAllowDropSchemaWhenDatabaseNameContains(expected: String): Unit =
    onlyAllowDropSchemaWhenDatabaseNameWhen(
      _ contains expected,
      name => s"You're trying to wipe $name, which is not allowed because it doesn't contain [$expected].")

  def onlyAllowDropSchemaWhenDatabaseNameEndsWith(suffix: String): Unit =
    onlyAllowDropSchemaWhenDatabaseNameWhen(
      _ endsWith suffix,
      name => s"You're trying to wipe $name, which is not allowed because it doesn't end with [$suffix].")

  def onlyAllowDropSchemaWhenDatabaseNameStartsWith(prefix: String): Unit =
    onlyAllowDropSchemaWhenDatabaseNameWhen(
      _ startsWith prefix,
      name => s"You're trying to wipe $name, which is not allowed because it doesn't start with [$prefix].")

  /** Drops all objects (tables, views, procedures, triggers, ...) in the configured schemas. */
  def dropSchema(): Unit = {
    init()
    rwlock.writeLock {
      val db = this.db()
      val dbName = db.databaseName
      onDropSchemaAttempt.get()(db)
      logger.info(s"Dropping schema in: $dbName")
      val migration = db.migration.withFlywayConfig(_.cleanDisabled(false))
      migration.drop.unsafeRun()
    }
  }

  // Don't expose because it allows downstream to execute multiple txns, all of which will be immediately rolled back
  // by the txn strategy.
  // eg. useXa { xa ! insert; xa ! select } results in two rollbacks, not one as you'd expect.
  private def useXa[A](mutex: Boolean)(f: TestXA => IO[A]): IO[A] = {
    init()
    val lock = if (mutex) rwlock.writeLock else rwlock.readLock
    lock.applyF(xaWithoutLockingIO.flatMap(xa => f(new TestXA(xa, lazyTables))))
  }

  private def impureUseXa[A](mutex: Boolean)(f: TestXA => A): A =
    useXa(mutex)(xa => IO(f(xa))).unsafeRun()

  def ![A](query: ConnectionIO[A]): A =
    useXa(mutex = false)(_(query)).unsafeRun()

  lazy val tables: Set[DbTable] = {
    val t = this ! DbTable.all(db().schema)
    logger.debug(t.toList.sortBy(_.name).iterator.map("  - " + _.name).mkString("Detected tables:\n", "\n", ""))
    t
  }

  val lazyTables = () => tables

  private def truncateAllWithoutLocking(): Unit = {
    val t = tables
    logger.info("Truncating all tables...")
    this.!(DbTable.truncateAll(t) *> C.commit)
  }

  def truncateAll(): Unit = {
    init()
    rwlock.writeLock {
      truncateAllWithoutLocking()
    }
  }

  // ===================================================================================================================
  // SQL checking

  def check[A: Analyzable](a: A): Unit =
    checkImpl(Analyzable.unpack(a))

  // Copied from Doobie
  private val packagePrefix = "\\b[a-z]+\\.".r

  private def typeName[A](implicit tag: Tag[A]): String =
    packagePrefix.replaceAllIn(tag.tag.toString, "")

  def checkOutput[A: Tag](q: Query0[A])(implicit l: Line): Unit =
    checkImpl(AnalysisArgs(s"Query0[${typeName[A]}]", q.pos, q.sql, q.outputAnalysis))

  def checkOutput[A: Tag, B: Tag](q: Query[A, B])(implicit l: Line): Unit =
    checkImpl(AnalysisArgs(s"Query[${typeName[A]}, ${typeName[B]}]", q.pos, q.sql, q.outputAnalysis))

  private def checkImpl(args: AnalysisArgs)(implicit l: Line): Unit =
    impureUseXa(mutex = false) { xa =>
      val report     = analyze(args).transact(xa.transactor).unsafeRunSync()
      def reportText = formatReport(args, report, Colors.Ansi).padLeft("  ").toString
      if (!report.succeeded)
        fail(reportText)
      println(reportText)
    }

  // ===================================================================================================================
  // Imperative

  private lazy val connection: Resource[IO, Connection] =
    Resource {
      init()
      IO {
        val lock = rwlock.readLock.lockInterruptibly()
        val c = db().dataSource.getConnection()

        val c2: Connection =
          new DelegateConnection(c) {
            override def close(): Unit = ()
          }

        val teardown = IO {
          try
            c.close()
          finally
            lock.unlock()
        }

        (c2, teardown)
      }
    }

  protected def singleConnStrategy =
    Strategy.void.copy(before = initConnection)

  private lazy val singleConnXA: Resource[IO, XA] =
    for {
      conn <- connection
    } yield {
      val x = Transactor.fromConnection[IO](conn, None).copy(strategy0 = singleConnStrategy)
      new XA(x)
    }

  /** Everything runs in a transaction and is automatically rolled back */
  def withImperativeXA[A](f: ImperativeXA => A): A = {
    init()
    // TODO: Use mutex
    singleConnXA.use { xa =>
      IO {
        val i = new ImperativeXA(xa, db(), lazyTables)
        i ! C.setAutoCommit(false)
        val s = i ! C.setSavepoint
        try
          f(i)
        finally
          i ! C.rollback(s)
      }
    }.unsafeRun()
  }

  /** Returns a connection that really commits transactions on completion and writes to the DB.
    *
    * Ensure that you call [[releaseRealXA()]] after use.
    *
    * Use with care.
    * Use as a last resort.
    */
  def acquireRealXA(): (ImperativeXA, SharedLock.Locked) = {
    import scala.language.existentials
    val l = rwlock.writeLock.lockInterruptibly()
    val t = xaWithoutLocking().transactor.copy(strategy0 = Strategy.default)
    val xa = new ImperativeXA(new XA(t), db(), lazyTables)
    (xa, l)
  }

  /** Provides a connection that really commits transactions on completion and writes to the DB. */
  def withRealXA[A](f: ImperativeXA => A): A = {
    val (xa, lock) = acquireRealXA()
    try
      f(xa)
    finally
      lock.unlock()
  }
}
