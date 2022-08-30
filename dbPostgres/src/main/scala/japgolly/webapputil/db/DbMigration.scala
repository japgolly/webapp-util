package japgolly.webapputil.db

import cats.effect.IO
import javax.sql.DataSource
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.configuration.FluentConfiguration

object DbMigration {

  def apply(ds    : DataSource,
            schema: Option[String] = None,
            flyway: FlywayConfig = FlywayConfig.default,
           ): DbMigration = {
    var cfg = flyway(Flyway.configure).dataSource(ds)
    schema.foreach(s => cfg = cfg.schemas(s))
    new DbMigration(cfg)
  }

  type FlywayConfig = FluentConfiguration => FluentConfiguration

  object FlywayConfig {
    def default: FlywayConfig = _
      .locations("db_migrations")
      .sqlMigrationPrefix("v")
  }
}

final class DbMigration(private val flywayCfg: FluentConfiguration) {

  private val flyway: Flyway =
    flywayCfg.load()

  def withFlywayConfig(f: DbMigration.FlywayConfig): DbMigration =
    new DbMigration(f(flywayCfg))

  def migrate: IO[Unit] =
    IO(flyway.migrate())

  /** Drops all objects (tables, views, procedures, triggers, ...) in the configured schemas. */
  def drop: IO[Unit] =
    IO(flyway.clean())
}
