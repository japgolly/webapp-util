package japgolly.webapputil.db.test

import cats.Eval
import japgolly.clearconfig._
import japgolly.webapputil.db.DbConfig

/** Extend this to create your own:
 *
 * {{{
 *   object TestConfig extends japgolly.webapputil.db.test.TestDbConfig {
 *     override protected def propsFilename =
 *       "blah.properties"
 *   }
 * }}}
 */
trait TestDbConfig {

  protected def propsPrefixDb =
    "db."

  protected def propsFilename =
    "test.properties"

  protected def propsFilenameOptional =
    true

  protected def configSources: ConfigSources[Eval] =
    // Highest pri
    ConfigSource.environment[Eval] >
    ConfigSource.propFileOnClasspath[Eval](propsFilename, optional = propsFilenameOptional) >
    ConfigSource.system[Eval]
    // Lowest pri

  protected def loadDbConfig[A](defn: ConfigDef[A]): A =
    defn.run(configSources).map(_.getOrDie()).value

  protected def dbAppName: Option[String] =
    None

  protected def dbConfig: ConfigDef[DbConfig] =
    DbConfig.config(defaultAppName = dbAppName).withPrefix(propsPrefixDb)

  protected def loadDbConfigOrThrow(): DbConfig =
    loadDbConfig(dbConfig)

  lazy val db: DbConfig =
    loadDbConfigOrThrow()
}
