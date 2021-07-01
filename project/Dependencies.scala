import sbt._
import sbt.Keys._
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._

object Dependencies {

  object Ver {

    // Exported
    def boopickle    = "1.3.3"
    def circe        = "0.14.1"
    def microlibs    = "2.6"
    def nyaya        = "0.10.0"
    def scala2       = "2.13.6"
    def scalaJsDom   = "1.1.0"
    def scalaJsReact = "1.7.7"
    def univEq       = "1.4.0"

    // Internal
    def utest        = "0.7.10"
  }


  object Dep {
    val boopickle           = Def.setting("io.suzaku"                         %%% "boopickle"     % Ver.boopickle)
    val circeCore           = Def.setting("io.circe"                          %%% "circe-core"    % Ver.circe)
    val circeParser         = Def.setting("io.circe"                          %%% "circe-parser"  % Ver.circe)
    val circeTesting        = Def.setting("io.circe"                          %%% "circe-testing" % Ver.circe)
    val microlibsAdtMacros  = Def.setting("com.github.japgolly.microlibs"     %%% "adt-macros"    % Ver.microlibs)
    val microlibsMacroUtils = Def.setting("com.github.japgolly.microlibs"     %%% "macro-utils"   % Ver.microlibs)
    val microlibsNonempty   = Def.setting("com.github.japgolly.microlibs"     %%% "nonempty"      % Ver.microlibs)
    val microlibsRecursion  = Def.setting("com.github.japgolly.microlibs"     %%% "recursion"     % Ver.microlibs)
    val microlibsScalazExt  = Def.setting("com.github.japgolly.microlibs"     %%% "scalaz-ext"    % Ver.microlibs)
    val microlibsStdlibExt  = Def.setting("com.github.japgolly.microlibs"     %%% "stdlib-ext"    % Ver.microlibs)
    val microlibsTestUtil   = Def.setting("com.github.japgolly.microlibs"     %%% "test-util"     % Ver.microlibs)
    val microlibsUtils      = Def.setting("com.github.japgolly.microlibs"     %%% "utils"         % Ver.microlibs)
    val nyayaGen            = Def.setting("com.github.japgolly.nyaya"         %%% "nyaya-gen"     % Ver.nyaya)
    val nyayaProp           = Def.setting("com.github.japgolly.nyaya"         %%% "nyaya-prop"    % Ver.nyaya)
    val nyayaTest           = Def.setting("com.github.japgolly.nyaya"         %%% "nyaya-test"    % Ver.nyaya)
    val scalaJsDom          = Def.setting("org.scala-js"                      %%% "scalajs-dom"   % Ver.scalaJsDom)
    val scalaJsReactCore    = Def.setting("com.github.japgolly.scalajs-react" %%% "core"          % Ver.scalaJsReact)
    val scalaJsReactExtra   = Def.setting("com.github.japgolly.scalajs-react" %%% "extra"         % Ver.scalaJsReact)
    val scalaJsReactTest    = Def.setting("com.github.japgolly.scalajs-react" %%% "test"          % Ver.scalaJsReact)
    val univEq              = Def.setting("com.github.japgolly.univeq"        %%% "univeq"        % Ver.univEq)
    val utest               = Def.setting("com.lihaoyi"                       %%% "utest"         % Ver.utest)
  }

}
