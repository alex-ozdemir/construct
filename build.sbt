enablePlugins(ScalaJSPlugin)


name := "Construct"

version := "0.1"

scalaVersion := "2.11.7"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

libraryDependencies += "org.scala-lang.modules" %% "scala-swing" % "1.0.1"
libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.5"
libraryDependencies += "org.scala-lang" % "scala-compiler" % scalaVersion.value
libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.12.5" % "test"
libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test"
libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.9.1"
libraryDependencies += "org.scala-lang.modules" %%% "scala-parser-combinators" % "1.0.5"

// This is an application with a main method
scalaJSUseMainModuleInitializer := false
mainClass in Compile := Some("construct.ConstructWebGREPL")
