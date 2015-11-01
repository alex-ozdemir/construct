name := "Construct"

version := "0.1"

scalaVersion := "2.11.7"

// libraryDependencies not used; instead we define a project dependency in
// // project/build.scala

libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4"
libraryDependencies += "org.scala-lang" % "scala-compiler" % scalaVersion.value
libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.12.5" % "test"
libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test"
