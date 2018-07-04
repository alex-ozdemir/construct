name := "construct-jvm"

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-compiler" % scalaVersion.value,
  "org.scala-lang.modules" %% "scala-swing" % "2.0.3",
)
