{
  if (Lib.scalafixEnabled)
    Seq(
      ThisBuild / semanticdbEnabled          := true,
      ThisBuild / scalafixScalaBinaryVersion := "2.13",
      ThisBuild / semanticdbVersion          := "4.4.24",

      ThisBuild / scalacOptions ++= {
        if (scalaVersion.value startsWith "2")
          "-Yrangepos" :: "-P:semanticdb:synthetics:on" :: Nil
        else
          Nil
      },

      ThisBuild / scalafixDependencies ++= Seq(
        "com.github.liancheng" %% "organize-imports" % "0.5.0"
      )
    )
  else
    Nil
}
