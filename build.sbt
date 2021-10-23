ThisBuild / homepage      := Some(url("https://github.com/japgolly/webapp-util"))
ThisBuild / licenses      := ("Apache-2.0", url("http://opensource.org/licenses/Apache-2.0")) :: Nil
ThisBuild / organization  := "com.github.japgolly.webapp-util"
ThisBuild / shellPrompt   := ((s: State) => Project.extract(s).currentRef.project + "> ")
ThisBuild / startYear     := Some(2021)
ThisBuild / versionScheme := Some("early-semver")

val root                 = Build.root
val protocolJS           = Build.protocolJS
val protocolJVM          = Build.protocolJVM
val protocolTestJS       = Build.protocolTestJS
val protocolTestJVM      = Build.protocolTestJVM
val protocolCirceJS      = Build.protocolCirceJS
val protocolCirceJVM     = Build.protocolCirceJVM
val protocolCirceTestJS  = Build.protocolCirceTestJS
val protocolCirceTestJVM = Build.protocolCirceTestJVM
val protocolOkHttp4      = Build.protocolOkHttp4
