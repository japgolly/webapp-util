addSbtPlugin("ch.epfl.scala"      % "sbt-scalafix"             % "0.11.0")
addSbtPlugin("com.github.sbt"     % "sbt-ci-release"           % "1.5.12")
addSbtPlugin("org.planet42"       % "laika-sbt"                % "0.19.3")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.3.2")
addSbtPlugin("org.scala-js"       % "sbt-jsdependencies"       % "1.0.2")
addSbtPlugin("org.scala-js"       % "sbt-scalajs"              % "1.13.2")


libraryDependencies += "org.scala-js" %% "scalajs-env-jsdom-nodejs" % "1.1.0"
