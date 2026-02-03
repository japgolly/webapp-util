addSbtPlugin("ch.epfl.scala"      % "sbt-scalafix"             % "0.14.5")
addSbtPlugin("com.github.sbt"     % "sbt-ci-release"           % "1.11.2")
addSbtPlugin("org.planet42"       % "laika-sbt"                % "0.19.3")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.3.2")
addSbtPlugin("org.scala-js"       % "sbt-jsdependencies"       % "1.0.2")
addSbtPlugin("org.scala-js"       % "sbt-scalajs"              % "1.20.1")


libraryDependencies += "org.scala-js" %% "scalajs-env-jsdom-nodejs" % "1.1.0"
