package japgolly.webapputil.db

import cats.effect.{IO, Resource}
import cats.~>
import com.typesafe.scalalogging.Logger
import com.zaxxer.hikari.HikariDataSource
import doobie._
import doobie.hikari.HikariTransactor
import japgolly.webapputil.cats.effect.ThreadUtilsIO
import javax.sql.DataSource
import retry._
import scala.annotation.nowarn
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

final case class Db(config: DbConfig, dataSource: DataSource, xa: Resource[IO, XA], migration: DbMigration) {
  import Db.logger

  def host: String =
    config.dataSource.host

  def databaseName: String =
    config.dataSource.database

  def schema: Option[String] =
    config.dataSource.schema

  def absoluteSchema: String =
    config.dataSource.schema.getOrElse("public")

  def schemaAsPrefix: String =
    config.dataSource.schema.map(_ + ".").getOrElse("")

  def desc: String =
    s"$host/$databaseName" + schema.map(":" + _).getOrElse("")

  val verifyConnectivity: IO[Unit] =
    IO {
      logger.info(s"Connecting to database: $desc")
      dataSource.getConnection().close()
    }

  val trans: (ConnectionIO ~> IO) =
    new (ConnectionIO ~> IO) {
      override def apply[A](fa: ConnectionIO[A]): IO[A] =
        xa.use(_(fa))
    }
}

object Db {

  private[Db] val logger: Logger =
    Logger[Db]

  /** When using docker-compose, sometimes the DB image needs more time to initialise. This adds a small retry. */
  def fromCfg(cfg: DbConfig): IO[Db] = {
    import RetryDetails._
    import RetryPolicies._

    @nowarn("cat=unused")
    def logError(err: Throwable, details: RetryDetails): IO[Unit] =
      details match {
        case _: WillDelayAndRetry => IO(logger.warn("Db initialisation failed. Retrying..."))
        case _: GivingUp          => IO(logger.warn("Db initialisation failed. Giving up."))
      }

    retryingOnAllErrors[Db](
      policy = limitRetries[IO](5) join constantDelay[IO](2.seconds),
      onError = logError,
    )(fromCfgWithoutRetry(cfg))
  }

  // This is in IO because HikariDataSource connects to the DB (and throws when unable) on construction.
  def fromCfgWithoutRetry(cfg: DbConfig): IO[Db] =
    IO {
      val poolSize = cfg.poolSize
      assert(poolSize >= 1, s"DB pool size is $poolSize but must be >= 1")

      val dataSrc = new HikariDataSource(cfg.hikariInstance())

      hikari(
        cfg         = cfg,
        dsMain      = dataSrc,
        dsMigration = dataSrc,
        connectEC   = ThreadUtilsIO.threadPool("HikariCP", logger)(_.withThreads(poolSize)),
      )
    }.flatten

  def hikari(cfg        : DbConfig,
             dsMain     : HikariDataSource,
             dsMigration: DataSource,
             connectEC  : Resource[IO, ExecutionContext],
            ): IO[Db] =
    generic(
      cfg         = cfg,
      dsMain      = dsMain,
      dsMigration = dsMigration,
      connectEC   = connectEC,
      transactor  = HikariTransactor[IO](dsMain, _),
    )

  def generic(cfg        : DbConfig,
              dsMain     : DataSource,
              dsMigration: DataSource,
              connectEC  : Resource[IO, ExecutionContext],
              transactor : ExecutionContext => Transactor[IO],
             ): IO[Db] =
    IO {
      val xaResource: Resource[IO, XA] =
        for {
          ec <- connectEC
        } yield {
          val xa = transactor(ec)
          new XA(xa)
        }

      val migrator = DbMigration(dsMigration, cfg.dataSource.schema)

      Db(cfg, dsMain, xaResource, migrator)
    }
}
