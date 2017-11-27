import scala.sys.process._

enablePlugins(ScalaJSPlugin)

val packagePage = taskKey[Unit]("Package the webpage")
val testPage = taskKey[Unit]("Build a test version of the webpage")

lazy val construct = crossProject.in(file(".")).
  settings(
    // other settings
    name := "Construct",
    version := "0.2",
    scalaVersion := "2.11.7",
    scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature"),
    libraryDependencies += "org.scala-lang.modules" %%% "scala-parser-combinators" % "1.0.5",
  )
  .jvmSettings(
    libraryDependencies += "org.scala-lang" % "scala-compiler" % scalaVersion.value,
    libraryDependencies += "org.scala-lang.modules" %% "scala-swing" % "1.0.1",
    libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.12.5" % Test,
    libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.4" % Test,
  )
  .jsSettings(
    libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.9.1",
    packagePage := {
      // Depend on fullOptJS
      (fullOptJS in Compile).value

      val log = streams.value.log

      val baseTarget = s"${(target in Compile).value}/scala-2.11/"
      s"rm -rf $baseTarget/classes/webpage/target/" ! log
      s"mkdir -p $baseTarget/classes/webpage/target/" ! log
      file(s"$baseTarget/construct-opt.js") #> file(s"$baseTarget/classes/webpage/target/construct-opt.js") ! log
      file(s"$baseTarget/construct-jsdeps.js") #> file(s"$baseTarget/classes/webpage/target/construct-jsdeps.js") ! log
      Process("sed" :: "-i" :: "" :: "-e" :: "s%-fastopt.js%-opt.js%" :: s"$baseTarget/classes/webpage/index.html" :: Nil, Path.userHome) ! log
      Process("tar" :: "-cf" :: "../webpage.tar" :: "webpage" :: Nil, file(s"$baseTarget/classes")) ! log
    },

    testPage := {
      // Depend on fullOptJS
      (fastOptJS in Compile).value

      val log = streams.value.log

      val baseTarget = s"${(target in Compile).value}/scala-2.11/"
      s"rm -rf $baseTarget/classes/webpage/target/" ! log
      s"mkdir -p $baseTarget/classes/webpage/target/" ! log
      file(s"$baseTarget/construct-fastopt.js") #> file(s"$baseTarget/classes/webpage/target/construct-fastopt.js") ! log
      file(s"$baseTarget/construct-jsdeps.js") #> file(s"$baseTarget/classes/webpage/target/construct-jsdeps.js") ! log
      Process("sed" :: "-i" :: "" :: "-e" :: "s%-opt.js%-fastopt.js%" :: s"$baseTarget/classes/webpage/index.html" :: Nil, Path.userHome) ! log
    }
  )


lazy val constructJVM = construct.jvm
lazy val constructJS = construct.js

// This is an application with a main method
// scalaJSUseMainModuleInitializer := true
// mainClass in Compile := Some("construct.ConstructWebGREPL")
