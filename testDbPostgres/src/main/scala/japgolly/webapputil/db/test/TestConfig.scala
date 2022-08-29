package japgolly.webapputil.db.test

import cats.Eval
import japgolly.clearconfig._
import japgolly.webapputil.db.DbConfig

abstract class TestConfig {

  protected def propsPrefix =
    "db."

  protected def propsFilename =
    "test.properties"

  protected def propsFilenameOptEvalnal =
    true

  protected def configSources: ConfigSources[Eval] =
    // Highest pri
    ConfigSource.environment[Eval] >
    ConfigSource.propFileOnClasspath[Eval](propsFilename, optional = propsFilenameOptEvalnal) >
    ConfigSource.system[Eval]
    // Lowest pri

  protected def loadConfig[A](defn: ConfigDef[A]): A =
    defn.run(configSources).map(_.getOrDie()).value

  protected def config: ConfigDef[DbConfig] =
    DbConfig.config.withPrefix(propsPrefix)

  protected def loadOrThrow(): DbConfig
    loadConfig(config)

  lazy val db: DbConfig =
    loadOrThrow()
}
