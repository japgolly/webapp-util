package japgolly.webapputil.db.test

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import japgolly.clearconfig._
import japgolly.webapputil.db.DbConfig

object TestConfig {

  private def configSources: ConfigSources[IO] =
    // Highest pri
    ConfigSource.environment[IO] >
    ConfigSource.propFileOnClasspath[IO]("webapp-test.properties", optional = false) >
    ConfigSource.system[IO]
    // Lowest pri

  private def loadConfig[A](defn: ConfigDef[A]): A =
    defn.run(configSources).map(_.getOrDie()).unsafeRunSync()

  lazy val db: DbConfig =
    loadConfig(DbConfig.config.withPrefix("db."))
}
