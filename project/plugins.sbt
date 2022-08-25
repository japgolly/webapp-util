addSbtPlugin("ch.epfl.scala"      % "sbt-scalafix"             % "0.10.1")
addSbtPlugin("com.github.sbt"     % "sbt-ci-release"           % "1.5.10")
addSbtPlugin("org.planet42"       % "laika-sbt"                % "0.18.2")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.2.0")
addSbtPlugin("org.scala-js"       % "sbt-jsdependencies"       % "1.0.2")
addSbtPlugin("org.scala-js"       % "sbt-scalajs"              % "1.10.1")


libraryDependencies += "org.scala-js" %% "scalajs-env-jsdom-nodejs" % "1.1.0"
