// shadow sbt-scalajs' crossProject and CrossType from Scala.js 0.6.x
import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}
import scala.sys.process._

scalaVersion := "2.12.6"

scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-feature",
  "-Xfatal-warnings",
  "-Xlint:all",
)

lazy val packagePage = taskKey[Unit]("Package an optimized version of the webpage")
lazy val testPage = taskKey[Unit]("Quickly build a test version of the webpage")

lazy val construct =
// select supported platforms
  crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Full)
    .in(file("."))
    .settings(
      scalaVersion := "2.12.6",
      libraryDependencies ++= Seq(
        "org.scala-lang" % "scala-compiler" % scalaVersion.value,
        "org.scala-lang.modules" %%% "scala-parser-combinators" % "1.0.5",
        "org.scalacheck" %% "scalacheck" % "1.14.0" % Test,
        "org.scalatest" %% "scalatest" % "3.0.5" % Test,
      ))
    .jsSettings(
      libraryDependencies ++= Seq(
        "org.scala-js" %%% "scalajs-dom" % "0.9.6",
      ),
      packagePage := {
        // Depend on fullOptJS
        (fullOptJS in Compile).value

        val log = streams.value.log
        val webpageRoot = s"${(classDirectory in Compile).value}/webpage/"
        val jsPath = (artifactPath in fullOptJS in Compile).value

        jsPath #> file(s"$webpageRoot/js/construct.js") ! log
        Process("tar" :: "-cf" :: "../webpage.tar" :: "webpage" :: Nil,
                file(s"${(classDirectory in Compile).value}")) ! log
      },
      testPage := {
        // Depend on fastOptJS
        (fastOptJS in Compile).value

        val log = streams.value.log
        val webpageRoot = s"${(classDirectory in Compile).value}/webpage/"
        val jsPath = (artifactPath in fastOptJS in Compile).value

        jsPath #> file(s"$webpageRoot/js/construct.js") ! log
      },
    ) // defined in sbt-scalajs-crossproject
    .jvmSettings(
      libraryDependencies ++= Seq(
        "org.scala-lang.modules" %% "scala-swing" % "2.0.3",
      ))

lazy val constructJS = construct.js
lazy val constructJVM = construct.jvm
