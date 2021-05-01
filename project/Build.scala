import sbt._
import sbt.Keys._
import com.jsuereth.sbtpgp.PgpKeys
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbtcrossproject.CrossPlugin.autoImport._
import sbtrelease.ReleasePlugin.autoImport._
import scalajscrossproject.ScalaJSCrossPlugin.autoImport._

object Build {
  import Dependencies._
  import Lib._

  private val ghProject = "webapp_protocols"

  private val publicationSettings =
    Lib.publicationSettings(ghProject)

  def scalacCommonFlags = Seq(
    "-deprecation",
    "-feature",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-language:postfixOps",
    "-unchecked",                                    // Enable additional warnings where generated code depends on assumptions.
    "-Wconf:msg=may.not.be.exhaustive:e",            // Make non-exhaustive matches errors instead of warnings
    "-Wdead-code",                                   // Warn when dead code is identified.
    "-Wunused:explicits",                            // Warn if an explicit parameter is unused.
    "-Wunused:implicits",                            // Warn if an implicit parameter is unused.
    "-Wunused:imports",                              // Warn if an import selector is not referenced.
    "-Wunused:locals",                               // Warn if a local definition is unused.
    "-Wunused:nowarn",                               // Warn if a @nowarn annotation does not suppress any warnings.
    "-Wunused:patvars",                              // Warn if a variable bound in a pattern is unused.
    "-Wunused:privates",                             // Warn if a private member is unused.
    "-Xlint:adapted-args",                           // An argument list was modified to match the receiver.
    "-Xlint:constant",                               // Evaluation of a constant arithmetic expression resulted in an error.
    "-Xlint:delayedinit-select",                     // Selecting member of DelayedInit.
    "-Xlint:deprecation",                            // Enable -deprecation and also check @deprecated annotations.
    "-Xlint:eta-zero",                               // Usage `f` of parameterless `def f()` resulted in eta-expansion, not empty application `f()`.
    "-Xlint:implicit-not-found",                     // Check @implicitNotFound and @implicitAmbiguous messages.
    "-Xlint:inaccessible",                           // Warn about inaccessible types in method signatures.
    "-Xlint:infer-any",                              // A type argument was inferred as Any.
    "-Xlint:missing-interpolator",                   // A string literal appears to be missing an interpolator id.
    "-Xlint:nonlocal-return",                        // A return statement used an exception for flow control.
    "-Xlint:nullary-unit",                           // `def f: Unit` looks like an accessor; add parens to look side-effecting.
    "-Xlint:option-implicit",                        // Option.apply used an implicit view.
    "-Xlint:poly-implicit-overload",                 // Parameterized overloaded implicit methods are not visible as view bounds.
    "-Xlint:private-shadow",                         // A private field (or class parameter) shadows a superclass field.
    "-Xlint:stars-align",                            // In a pattern, a sequence wildcard `_*` should match all of a repeated parameter.
    "-Xlint:valpattern",                             // Enable pattern checks in val definitions.
    "-Xmixin-force-forwarders:false",                // Only generate mixin forwarders required for program correctness.
    "-Xno-forwarders",                               // Do not generate static forwarders in mirror classes.
    "-Yjar-compression-level", "9",                  // compression level to use when writing jar files
    "-Yno-generic-signatures",                       // Suppress generation of generic signatures for Java.
    "-Ypatmat-exhaust-depth", "off"
  )

  val commonSettings = ConfigureBoth(
    _.settings(
      releasePublishArtifactsAction := PgpKeys.publishSigned.value,
      releaseTagComment             := s"v${(ThisBuild / version).value}",
      releaseVcsSign                := true,
      scalacOptions                ++= scalacCommonFlags,
      scalaVersion                  := Ver.scala213,
      Test / scalacOptions         --= Seq("-Ywarn-dead-code"),
      testFrameworks                := Nil,
      updateOptions                 := updateOptions.value.withCachedResolution(true),
    ))

  def testSettings = ConfigureBoth(
    _.settings(
      testFrameworks += new TestFramework("utest.runner.Framework"),
      libraryDependencies ++= Seq(
        Dep.microlibsTestUtil.value % Test,
        Dep.nyayaGen.value % Test,
        Dep.nyayaProp.value % Test,
        Dep.nyayaTest.value % Test,
        Dep.utest.value % Test,
      ),
    ))
    .jsConfigure(_.settings(
      Test / jsEnv := new JSDOMNodeJSEnv,
    ))

  lazy val root = project
    .in(file("."))
    .configure(commonSettings.jvm, preventPublication)
    .aggregate(
      coreJVM, coreJS,
      coreTestJVM, coreTestJS,
      circeJVM, circeJS,
      circeTestJVM, circeTestJS,
    )

  lazy val coreJVM = core.jvm
  lazy val coreJS  = core.js
  lazy val core = crossProject(JSPlatform, JVMPlatform)
    .configureCross(commonSettings, publicationSettings, testSettings)
    .settings(
      libraryDependencies += Dep.univEq.value,
    )
    .jsSettings(
      libraryDependencies ++= Seq(
        Dep.scalaJsDom.value,
        Dep.scalaJsReactCore.value,
        Dep.scalaJsReactExtra.value,
      ),
    )

  lazy val coreTestJVM = coreTest.jvm
  lazy val coreTestJS  = coreTest.js
  lazy val coreTest = crossProject(JSPlatform, JVMPlatform)
    .configureCross(commonSettings, publicationSettings, testSettings)
    .dependsOn(core)
    .settings(moduleName := "core-test")

  lazy val circeJVM = circe.jvm
  lazy val circeJS  = circe.js
  lazy val circe = crossProject(JSPlatform, JVMPlatform)
    .configureCross(commonSettings, publicationSettings, testSettings)
    .dependsOn(core)
    .settings(
      libraryDependencies ++= Seq(
        Dep.circeCore.value,
        Dep.microlibsAdtMacros.value,
        Dep.microlibsRecursion.value,
        Dep.microlibsUtils.value,
      ),
    )

  lazy val circeTestJVM = circeTest.jvm
  lazy val circeTestJS  = circeTest.js
  lazy val circeTest = crossProject(JSPlatform, JVMPlatform)
    .configureCross(commonSettings, publicationSettings, testSettings)
    .dependsOn(circe, coreTest)
    .settings(
      moduleName := "circe-test",
      libraryDependencies ++= Seq(
        Dep.circeParser.value,
        Dep.microlibsTestUtil.value,
        Dep.nyayaGen.value,
      ),
    )
}
