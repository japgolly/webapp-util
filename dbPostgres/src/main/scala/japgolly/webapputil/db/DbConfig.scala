package japgolly.webapputil.db

import cats.instances.vector._
import cats.syntax.all._
import com.zaxxer.hikari.HikariConfig
import japgolly.clearconfig._
import japgolly.microlibs.stdlib_ext.ParseInt
import japgolly.webapputil.general.ErrorMsg
import javax.sql.DataSource
import org.postgresql.ds.PGSimpleDataSource

final case class DbConfig(dataSource          : DbConfig.DataSource,
                          hikari              : DbConfig.Hikari,
                          hikariDataSourceMods: List[javax.sql.DataSource => javax.sql.DataSource],
                          logSql              : Boolean) {

  def dataSourceInstance(): DataSource = {
    var ds: DataSource = dataSource.instance()
    sqlTracer.foreach(t => ds = t.inject(ds))
    ds
  }

  def hikariInstance(): HikariConfig = {
    val ds = hikariDataSourceMods.foldLeft(dataSourceInstance())((x, f) => f(x))
    val h = hikari.instance()
    h.setDataSource(ds)
    h.setUsername(dataSource.username)
    h.setPassword(dataSource.password)
    h
  }

  def modifyHikariDataSource(f: DataSource => DataSource): DbConfig =
    copy(hikariDataSourceMods = hikariDataSourceMods :+ f)

  def poolSize: Int =
    hikari.maximumPoolSize

  lazy val sqlTracer: Option[SqlTracer] =
    Option.when[SqlTracer](logSql)(JdbcLogging)
}

object DbConfig {

  def config: ConfigDef[DbConfig] =
    config(None)

  def config(defaultAppName: String): ConfigDef[DbConfig] =
    config(Some(defaultAppName))

  def config(defaultAppName: Option[String]): ConfigDef[DbConfig] =
    (
      DataSource.config(defaultAppName),
      Hikari.config.withPrefix("pool."),
      ConfigDef.getOrUse("log.sql", true),
    ).mapN { (ds, hikari, logSql) =>
      DbConfig(ds, hikari, Nil, logSql)
    }.ensure(
      c => !(c.dataSource.schema.isDefined && c.hikari.searchPath.isDefined),
      "You can't set both the DB schema and search_path."
    )

  // ===================================================================================================================

  def validateSqlIdentifier(i: String): Option[ErrorMsg] =
    Option.unless(i.matches("[\\p{IsAlphabetic}_][\\p{IsAlphabetic}_$0-9]*"))(ErrorMsg(
      "SQL identifiers must begin with a letter or an underscore. Subsequent characters can be letters, underscores, digits, or dollar signs."
    ))

  // ===================================================================================================================

  final case class DataSource(
    appname               : Option[String],
    binaryTransfer        : Option[Boolean],
    binaryTransferDisable : Option[String],
    binaryTransferEnable  : Option[String],
    database              : String,
    disableColumnSanitiser: Option[Boolean],
    host                  : String,
    loginTimeout          : Option[Int],
    password              : String,
    ports                 : Vector[Int],
    prepareThreshold      : Option[Int],
    protocolVersion       : Option[Int],
    receiveBufferSize     : Option[Int],
    schema                : Option[String],
    sendBufferSize        : Option[Int],
    socketTimeout         : Option[Int],
    ssl                   : Option[Boolean],
    sslfactory            : Option[String],
    tcpKeepAlive          : Option[Boolean],
    unknownLength         : Option[Int],
    username              : String,
  ) {
    def instance(): PGSimpleDataSource = {
      val ds = new PGSimpleDataSource
      ds.setDatabaseName(database)
      ds.setUser(username)
      ds.setPassword(password)
      ds.setDisableColumnSanitiser(true)
      appname.foreach(ds.setApplicationName)
      binaryTransfer.foreach(ds.setBinaryTransfer)
      binaryTransferDisable.foreach(ds.setBinaryTransferDisable)
      binaryTransferEnable.foreach(ds.setBinaryTransferEnable)
      disableColumnSanitiser.foreach(ds.setDisableColumnSanitiser)
      loginTimeout.foreach(ds.setLoginTimeout)
      if (ports.nonEmpty) ds.setPortNumbers(ports.toArray)
      prepareThreshold.foreach(ds.setPrepareThreshold)
      protocolVersion.foreach(ds.setProtocolVersion)
      receiveBufferSize.foreach(ds.setReceiveBufferSize)
      sendBufferSize.foreach(ds.setSendBufferSize)
      schema.foreach(ds.setCurrentSchema)
      socketTimeout.foreach(ds.setSocketTimeout)
      ssl.foreach(ds.setSsl)
      sslfactory.foreach(ds.setSslfactory)
      tcpKeepAlive.foreach(ds.setTcpKeepAlive)
      unknownLength.foreach(ds.setUnknownLength)
      ds
    }
  }

  object DataSource {
    def config: ConfigDef[DataSource] =
      config(None)

    def config(defaultAppName: String): ConfigDef[DataSource] =
      config(Some(defaultAppName))

    def config(defaultAppName: Option[String]): ConfigDef[DataSource] = {
      implicit val vectorInt: ConfigValueParser[Vector[Int]] =
        ConfigValueParser(_.split(',').map(_.trim).filter(_.nonEmpty).toVector.traverse {
          case ParseInt(i) => Right(i)
          case s           => Left("Invalid integer: " + s)
        })

      val binaryTransfer         = ConfigDef.get[Boolean]("binaryTransfer")
      val binaryTransferDisable  = ConfigDef.get[String]("binaryTransferDisable")
      val binaryTransferEnable   = ConfigDef.get[String]("binaryTransferEnable")
      val database               = ConfigDef.need[String]("database")
      val disableColumnSanitiser = ConfigDef.get[Boolean]("disableColumnSanitiser")
      val host                   = ConfigDef.getOrUse("host", "localhost")
      val loginTimeout           = ConfigDef.get[Int]("loginTimeout")
      val password               = ConfigDef.need[String]("password")
      val ports                  = ConfigDef.getOrParseOrThrow[Vector[Int]]("port", "5432")
      val prepareThreshold       = ConfigDef.get[Int]("prepareThreshold")
      val protocolVersion        = ConfigDef.get[Int]("protocolVersion")
      val receiveBufferSize      = ConfigDef.get[Int]("receiveBufferSize")
      val sendBufferSize         = ConfigDef.get[Int]("sendBufferSize")
      val socketTimeout          = ConfigDef.get[Int]("socketTimeout")
      val ssl                    = ConfigDef.get[Boolean]("ssl")
      val sslfactory             = ConfigDef.get[String]("sslfactory")
      val tcpKeepAlive           = ConfigDef.get[Boolean]("tcpKeepAlive")
      val unknownLength          = ConfigDef.get[Int]("unknownLength")
      val username               = ConfigDef.need[String]("username")

      val appname: ConfigDef[Option[String]] =
        defaultAppName match {
          case None    => ConfigDef.get[String]("appname")
          case Some(a) => ConfigDef.getOrUse[String]("appname", a).map(Some(_))
        }

      val schema: ConfigDef[Option[String]] =
        ConfigDef.get[String]("schema").test(_.flatMap(validateSqlIdentifier).map(_.value))

      (
        appname,
        binaryTransfer,
        binaryTransferDisable,
        binaryTransferEnable,
        database,
        disableColumnSanitiser,
        host,
        loginTimeout,
        password,
        ports,
        prepareThreshold,
        protocolVersion,
        receiveBufferSize,
        schema,
        sendBufferSize,
        socketTimeout,
        ssl,
        sslfactory,
        tcpKeepAlive,
        unknownLength,
        username,
      ).mapN(apply)
    }
  }

  // ===================================================================================================================

  final case class Hikari(
    allowPoolSuspension      : Option[Boolean],
    catalog                  : Option[String],
    connectionInitSql        : Option[String],
    connectionTestQuery      : Option[String],
    connectionTimeout        : Option[Long],
    idleTimeout              : Option[Long],
    initializationFailTimeout: Option[Long],
    isolateInternalQueries   : Option[Boolean],
    keepaliveTime            : Option[Long],
    leakDetectionThreshold   : Option[Long],
    maximumPoolSize          : Int,
    maxLifetime              : Option[Long],
    minimumIdle              : Option[Int],
    poolName                 : Option[String],
    registerMbeans           : Option[Boolean],
    searchPath               : Option[String],
    transactionIsolation     : String,
    validationTimeout        : Option[Long],
  ) {

    def instance(): HikariConfig = {
      val h = new HikariConfig

      h.setAutoCommit(true)
      h.setMaximumPoolSize(maximumPoolSize)
      h.setTransactionIsolation(transactionIsolation)

      allowPoolSuspension.foreach(h.setAllowPoolSuspension)
      catalog.foreach(h.setCatalog)
      connectionInitSql.foreach(h.setConnectionInitSql)
      connectionTestQuery.foreach(h.setConnectionTestQuery)
      connectionTimeout.foreach(h.setConnectionTimeout)
      idleTimeout.foreach(h.setIdleTimeout)
      initializationFailTimeout.foreach(h.setInitializationFailTimeout)
      isolateInternalQueries.foreach(h.setIsolateInternalQueries)
      keepaliveTime.foreach(h.setKeepaliveTime)
      leakDetectionThreshold.foreach(h.setLeakDetectionThreshold)
      maxLifetime.foreach(h.setMaxLifetime)
      minimumIdle.foreach(h.setMinimumIdle)
      poolName.foreach(h.setPoolName)
      registerMbeans.foreach(h.setRegisterMbeans)
      searchPath.foreach(s => h.setConnectionInitSql(s"SET search_path TO $s"))
      validationTimeout.foreach(h.setValidationTimeout)

      h
    }
  }

  object Hikari {
    def config: ConfigDef[Hikari] = {

      val allowPoolSuspension       = ConfigDef.get[Boolean]("allowPoolSuspension")
      val catalog                   = ConfigDef.get[String]("catalog")
      val connectionInitSql         = ConfigDef.get[String]("connectionInitSql")
      val connectionTestQuery       = ConfigDef.get[String]("connectionTestQuery")
      val connectionTimeout         = ConfigDef.get[Long]("connectionTimeout")
      val idleTimeout               = ConfigDef.get[Long]("idleTimeout")
      val initializationFailTimeout = ConfigDef.get[Long]("initializationFailTimeout")
      val isolateInternalQueries    = ConfigDef.get[Boolean]("isolateInternalQueries")
      val keepaliveTime             = ConfigDef.get[Long]("keepaliveTime")
      val leakDetectionThreshold    = ConfigDef.get[Long]("leakDetectionThreshold")
      val maximumPoolSize           = ConfigDef.need[Int]("maximumPoolSize")
      val maxLifetime               = ConfigDef.get[Long]("maxLifetime")
      val minimumIdle               = ConfigDef.get[Int]("minimumIdle")
      val poolName                  = ConfigDef.get[String]("poolName")
      val registerMbeans            = ConfigDef.get[Boolean]("registerMbeans")
      val searchPath                = ConfigDef.get[String]("search_path").test(_.flatMap(validateSqlIdentifier).map(_.value))
      val transactionIsolation      = ConfigDef.getOrUse("transactionIsolation", "TRANSACTION_REPEATABLE_READ")
      val validationTimeout         = ConfigDef.get[Long]("validationTimeout")

      (
        allowPoolSuspension,
        catalog,
        connectionInitSql,
        connectionTestQuery,
        connectionTimeout,
        idleTimeout,
        initializationFailTimeout,
        isolateInternalQueries,
        keepaliveTime,
        leakDetectionThreshold,
        maximumPoolSize,
        maxLifetime,
        minimumIdle,
        poolName,
        registerMbeans,
        searchPath,
        transactionIsolation,
        validationTimeout,
      ).mapN(apply)
    }

    // _.get("autoCommit"              , _.setAutoCommit),
    // _.get("dataSource"              , _.setDataSource),
    // _.get("dataSourceClassName"     , _.setDataSourceClassName),
    // _.get("dataSourceJNDI"          , _.setDataSourceJNDI),
    // _.get("dataSourceProperties"    , _.setDataSourceProperties),
    // _.get("driverClassName"         , _.setDriverClassName),
    // _.get("jdbc4ConnectionTest"     , _.setJdbc4ConnectionTest),
    // _.get("jdbcUrl"                 , _.setJdbcUrl),
    // _.get("metricsTrackerFactory"   , _.setMetricsTrackerFactory),
    // _.get("metricRegistry"          , _.setMetricRegistry),
    // _.get("healthCheckRegistry"     , _.setHealthCheckRegistry),
    // _.get("healthCheckProperties"   , _.setHealthCheckProperties),
    // _.get("password"                , _.setPassword),
    // _.get("readOnly"                , _.setReadOnly),
    // _.get("scheduledExecutorService", _.setScheduledExecutorService),
    // _.get("threadFactory"           , _.setThreadFactory),
    // _.get("username"                , _.setUsername),
  }

}
