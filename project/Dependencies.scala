import sbt._
import sbt.Keys._
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import org.scalajs.jsdependencies.sbtplugin.JSDependenciesPlugin
import org.scalajs.jsdependencies.sbtplugin.JSDependenciesPlugin.autoImport._

object Dependencies {

  object Ver {

    // Exported
    def boopickle         = "1.4.0"
    def catsEffect        = "3.3.11"
    def catsRetry         = "3.1.0"
    def circe             = "0.14.1"
    def clearConfig       = "3.0.0"
    def doobie            = "1.0.0-RC2"
    def flyway            = "8.0.5"
    def hikariCP          = "4.0.3"
    def izumiReflect      = "2.1.0"
    def javaxWebsocketApi = "1.0"
    def microlibs         = "4.1.0"
    def nyaya             = "1.0.0"
    def okHttp4           = "4.9.3"
    def postgresql        = "42.3.5"
    def scala2            = "2.13.8"
    def scala3            = "3.1.2"
    def scalaJsDom        = "2.1.0"
    def scalaJsReact      = "2.1.1"
    def scalaLogging      = "3.9.4"
    def testState         = "3.0.0"
    def univEq            = "2.0.0"

    // Internal
    def base32768           = "2.0.2"
    def betterMonadicFor    = "0.3.1"
    def kindProjector       = "0.13.2"
    def pako                = "2.0.4"
    def reactJs             = "17.0.2"
    def scalaJsJavaTime     = "1.0.0"
    def scalaJsSecureRandom = "1.0.0"
    def utest               = "0.7.11"
  }

  object Dep {
    val boopickle           = Def.setting("io.suzaku"                         %%% "boopickle"                               % Ver.boopickle)
    val catsEffect          = Def.setting("org.typelevel"                     %%% "cats-effect"                             % Ver.catsEffect)
    val catsRetry           = Def.setting("com.github.cb372"                   %% "cats-retry"                              % Ver.catsRetry)
    val circeCore           = Def.setting("io.circe"                          %%% "circe-core"                              % Ver.circe)
    val circeParser         = Def.setting("io.circe"                          %%% "circe-parser"                            % Ver.circe)
    val circeTesting        = Def.setting("io.circe"                          %%% "circe-testing"                           % Ver.circe)
    val clearConfig         = Def.setting("com.github.japgolly.clearconfig"   %%% "core"                                    % Ver.clearConfig)
    val doobieCore          = Def.setting("org.tpolecat"                      %%% "doobie-core"                             % Ver.doobie)
    val doobieHikari        = Def.setting("org.tpolecat"                      %%% "doobie-hikari"                           % Ver.doobie)
    val doobiePostgres      = Def.setting("org.tpolecat"                      %%% "doobie-postgres"                         % Ver.doobie)
    val doobiePostgresCirce = Def.setting("org.tpolecat"                      %%% "doobie-postgres-circe"                   % Ver.doobie)
    val flyway              = Def.setting("org.flywaydb"                        % "flyway-core"                             % Ver.flyway)
    val hikariCP            = Def.setting("com.zaxxer"                          % "HikariCP"                                % Ver.hikariCP)
    val izumiReflect        = Def.setting("dev.zio"                            %% "izumi-reflect"                           % Ver.izumiReflect)
    val javaxWebsocketApi   = Def.setting("javax.websocket"                     % "javax.websocket-api"                     % Ver.javaxWebsocketApi)
    val microlibsAdtMacros  = Def.setting("com.github.japgolly.microlibs"     %%% "adt-macros"                              % Ver.microlibs)
    val microlibsNonEmpty   = Def.setting("com.github.japgolly.microlibs"     %%% "nonempty"                                % Ver.microlibs)
    val microlibsRecursion  = Def.setting("com.github.japgolly.microlibs"     %%% "recursion"                               % Ver.microlibs)
    val microlibsStdlibExt  = Def.setting("com.github.japgolly.microlibs"     %%% "stdlib-ext"                              % Ver.microlibs)
    val microlibsTestUtil   = Def.setting("com.github.japgolly.microlibs"     %%% "test-util"                               % Ver.microlibs)
    val microlibsUtils      = Def.setting("com.github.japgolly.microlibs"     %%% "utils"                                   % Ver.microlibs)
    val nyayaGen            = Def.setting("com.github.japgolly.nyaya"         %%% "nyaya-gen"                               % Ver.nyaya)
    val nyayaProp           = Def.setting("com.github.japgolly.nyaya"         %%% "nyaya-prop"                              % Ver.nyaya)
    val nyayaTest           = Def.setting("com.github.japgolly.nyaya"         %%% "nyaya-test"                              % Ver.nyaya)
    val okHttp4             = Def.setting("com.squareup.okhttp3"                % "okhttp"                                  % Ver.okHttp4)
    val postgresql          = Def.setting("org.postgresql"                      % "postgresql"                              % Ver.postgresql)
    val scalaJsDom          = Def.setting("org.scala-js"                      %%% "scalajs-dom"                             % Ver.scalaJsDom)
    val scalaJsJavaTime     = Def.setting("org.scala-js"                      %%% "scalajs-java-time"                       % Ver.scalaJsJavaTime cross CrossVersion.for3Use2_13)
    val scalaJsReactCore    = Def.setting("com.github.japgolly.scalajs-react" %%% "core"                                    % Ver.scalaJsReact)
    val scalaJsReactExtra   = Def.setting("com.github.japgolly.scalajs-react" %%% "extra"                                   % Ver.scalaJsReact)
    val scalaJsReactTest    = Def.setting("com.github.japgolly.scalajs-react" %%% "test"                                    % Ver.scalaJsReact)
    val scalaJsSecureRandom = Def.setting("org.scala-js"                      %%% "scalajs-fake-insecure-java-securerandom" % Ver.scalaJsSecureRandom cross CrossVersion.for3Use2_13)
    val scalaLogging        = Def.setting("com.typesafe.scala-logging"         %% "scala-logging"                           % Ver.scalaLogging)
    val testStateCore       = Def.setting("com.github.japgolly.test-state"    %%% "core"                                    % Ver.testState)
    val univEq              = Def.setting("com.github.japgolly.univeq"        %%% "univeq"                                  % Ver.univEq)
    val utest               = Def.setting("com.lihaoyi"                       %%% "utest"                                   % Ver.utest)

    def base32768(c: Configuration) = Def.setting("org.webjars.npm" % "base32768" % Ver.base32768 % c / "dist/iife/base32768.js" commonJSName "base32768")
    def pako     (c: Configuration) = Def.setting("org.webjars.npm" % "pako"      % Ver.pako      % c / "dist/pako.min.js"       commonJSName "pako")

    val react             = ReactArtifact("react")
    val reactDom          = ReactArtifact("react-dom")
    val reactDomServer    = ReactArtifact("react-dom-server.browser")
    val reactDoutestUtils = ReactArtifact("react-dom-test-utils")

    val betterMonadicFor = compilerPlugin("com.olegpy"    %% "better-monadic-for" % Ver.betterMonadicFor)
    val kindProjector    = compilerPlugin("org.typelevel" %% "kind-projector"     % Ver.kindProjector cross CrossVersion.full)
  }

  final case class ReactArtifact(filename: String) {
    val dev = s"umd/$filename.development.js"
    val prod = s"umd/$filename.production.min.js"
  }

  def addReactJsDependencies(scope: Configuration): Project => Project =
    _.enablePlugins(JSDependenciesPlugin)
      .settings(
        jsDependencies ++= Seq(

          "org.webjars.npm" % "react" % Ver.reactJs % scope
            /         "umd/react.development.js"
            minified  "umd/react.production.min.js"
            commonJSName "React",

          "org.webjars.npm" % "react-dom" % Ver.reactJs % scope
            /         "umd/react-dom.development.js"
            minified  "umd/react-dom.production.min.js"
            dependsOn "umd/react.development.js"
            commonJSName "ReactDOM",

          "org.webjars.npm" % "react-dom" % Ver.reactJs % scope
            /         "umd/react-dom-test-utils.development.js"
            minified  "umd/react-dom-test-utils.production.min.js"
            dependsOn "umd/react-dom.development.js"
            commonJSName "ReactTestUtils",

          "org.webjars.npm" % "react-dom" % Ver.reactJs % scope
            /         "umd/react-dom-server.browser.development.js"
            minified  "umd/react-dom-server.browser.production.min.js"
            dependsOn "umd/react-dom.development.js"
            commonJSName "ReactDOMServer"),

        packageJSDependencies / skip := false)
}
