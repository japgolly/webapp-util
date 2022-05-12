ThisBuild / homepage      := Some(url("https://github.com/japgolly/webapp-util"))
ThisBuild / licenses      := ("Apache-2.0", url("http://opensource.org/licenses/Apache-2.0")) :: Nil
ThisBuild / organization  := "com.github.japgolly.webapp-util"
ThisBuild / shellPrompt   := ((s: State) => Project.extract(s).currentRef.project + "> ")
ThisBuild / startYear     := Some(2021)
ThisBuild / versionScheme := Some("early-semver")

val root              = Build.root

val coreJS            = Build.coreJS
val coreJVM           = Build.coreJVM
val testCoreJS        = Build.testCoreJS
val testCoreJVM       = Build.testCoreJVM

val testNode          = Build.testNode

val coreBoopickleJS   = Build.coreBoopickleJS
val coreBoopickleJVM  = Build.coreBoopickleJVM
val testBoopickleJS   = Build.testBoopickleJS
val testBoopickleJVM  = Build.testBoopickleJVM

val coreCatsEffectJS  = Build.coreCatsEffectJS
val coreCatsEffectJVM = Build.coreCatsEffectJVM
val testCatsEffectJS  = Build.testCatsEffectJS
val testCatsEffectJVM = Build.testCatsEffectJVM

val coreCirceJS       = Build.coreCirceJS
val coreCirceJVM      = Build.coreCirceJVM
val testCirceJS       = Build.testCirceJS
val testCirceJVM      = Build.testCirceJVM

val coreOkHttp4       = Build.coreOkHttp4

val dbPostgres        = Build.dbPostgres
val testDbPostgres    = Build.testDbPostgres

val examplesJVM       = Build.examplesJVM
val examplesJS        = Build.examplesJS
