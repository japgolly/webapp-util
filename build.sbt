ThisBuild / homepage     := Some(url("https://github.com/japgolly/webapp_protocols"))
ThisBuild / licenses     := ("Apache-2.0", url("http://opensource.org/licenses/Apache-2.0")) :: Nil
ThisBuild / organization := "com.github.japgolly.webapp_protocols"
ThisBuild / shellPrompt  := ((s: State) => Project.extract(s).currentRef.project + "> ")
ThisBuild / startYear    := Some(2021)

val root        = Build.root
val coreJS      = Build.coreJS
val coreJVM     = Build.coreJVM
val coreTestJS  = Build.coreTestJS
val coreTestJVM = Build.coreTestJVM
