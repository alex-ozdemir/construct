scalaVersion := "2.12.6"

scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-feature",
  "-Xfatal-warnings",
)

lazy val root = (project in file("."))
  .aggregate(js, jvm, shared)

lazy val shared = project

lazy val jvm = project
  .dependsOn(shared)
lazy val js = project
  .dependsOn(shared)

