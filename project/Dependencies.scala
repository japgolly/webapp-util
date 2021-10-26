import sbt._
import sbt.Keys._
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._

object Dependencies {

  object Ver {

    // Exported
    def boopickle    = "1.4.0"
    def catsEffect   = "3.2.9"
    def catsRetry    = "3.1.0"
    def circe        = "0.14.1"
    def clearConfig  = "3.0.0"
    def doobie       = "1.0.0-RC1"
    def flyway       = "8.0.2"
    def hikariCP     = "4.0.3"
    def izumiReflect = "2.0.0"
    def microlibs    = "4.0.0"
    def nyaya        = "1.0.0"
    def okHttp4      = "4.9.2"
    def postgresql   = "42.3.0"
    def scala2       = "2.13.6"
    def scala3       = "3.0.2"
    def scalaJsDom   = "2.0.0"
    def scalaJsReact = "2.0.0-RC4"
    def scalaLogging = "3.9.4"
    def univEq       = "2.0.0"

    // Internal
    def utest        = "0.7.10"
  }

  object Dep {
    val boopickle           = Def.setting("io.suzaku"                         %%% "boopickle"             % Ver.boopickle)
    val catsEffect          = Def.setting("org.typelevel"                     %%% "cats-effect"           % Ver.catsEffect)
    val catsRetry           = Def.setting("com.github.cb372"                   %% "cats-retry"            % Ver.catsRetry)
    val circeCore           = Def.setting("io.circe"                          %%% "circe-core"            % Ver.circe)
    val circeParser         = Def.setting("io.circe"                          %%% "circe-parser"          % Ver.circe)
    val circeTesting        = Def.setting("io.circe"                          %%% "circe-testing"         % Ver.circe)
    val clearConfig         = Def.setting("com.github.japgolly.clearconfig"   %%% "core"                  % Ver.clearConfig)
    val doobieCore          = Def.setting("org.tpolecat"                      %%% "doobie-core"           % Ver.doobie)
    val doobieHikari        = Def.setting("org.tpolecat"                      %%% "doobie-hikari"         % Ver.doobie)
    val doobiePostgres      = Def.setting("org.tpolecat"                      %%% "doobie-postgres"       % Ver.doobie)
    val doobiePostgresCirce = Def.setting("org.tpolecat"                      %%% "doobie-postgres-circe" % Ver.doobie)
    val flyway              = Def.setting("org.flywaydb"                        % "flyway-core"           % Ver.flyway)
    val hikariCP            = Def.setting("com.zaxxer"                          % "HikariCP"              % Ver.hikariCP)
    val izumiReflect        = Def.setting("dev.zio"                            %% "izumi-reflect"         % Ver.izumiReflect)
    val microlibsAdtMacros  = Def.setting("com.github.japgolly.microlibs"     %%% "adt-macros"            % Ver.microlibs)
    val microlibsRecursion  = Def.setting("com.github.japgolly.microlibs"     %%% "recursion"             % Ver.microlibs)
    val microlibsTestUtil   = Def.setting("com.github.japgolly.microlibs"     %%% "test-util"             % Ver.microlibs)
    val microlibsUtils      = Def.setting("com.github.japgolly.microlibs"     %%% "utils"                 % Ver.microlibs)
    val nyayaGen            = Def.setting("com.github.japgolly.nyaya"         %%% "nyaya-gen"             % Ver.nyaya)
    val nyayaProp           = Def.setting("com.github.japgolly.nyaya"         %%% "nyaya-prop"            % Ver.nyaya)
    val nyayaTest           = Def.setting("com.github.japgolly.nyaya"         %%% "nyaya-test"            % Ver.nyaya)
    val okHttp4             = Def.setting("com.squareup.okhttp3"                % "okhttp"                % Ver.okHttp4)
    val postgresql          = Def.setting("org.postgresql"                      % "postgresql"            % Ver.postgresql)
    val scalaJsDom          = Def.setting("org.scala-js"                      %%% "scalajs-dom"           % Ver.scalaJsDom)
    val scalaJsReactCore    = Def.setting("com.github.japgolly.scalajs-react" %%% "core"                  % Ver.scalaJsReact)
    val scalaJsReactExtra   = Def.setting("com.github.japgolly.scalajs-react" %%% "extra"                 % Ver.scalaJsReact)
    val scalaJsReactTest    = Def.setting("com.github.japgolly.scalajs-react" %%% "test"                  % Ver.scalaJsReact)
    val scalaLogging        = Def.setting("com.typesafe.scala-logging"         %% "scala-logging"         % Ver.scalaLogging)
    val univEq              = Def.setting("com.github.japgolly.univeq"        %%% "univeq"                % Ver.univEq)
    val utest               = Def.setting("com.lihaoyi"                       %%% "utest"                 % Ver.utest)
  }
}
