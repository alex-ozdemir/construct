import scala.sys.process._

name := "construct-js"

enablePlugins(ScalaJSPlugin)

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-compiler" % scalaVersion.value,
  "org.scala-js" %%% "scalajs-dom" % "0.9.6",
)

val packagePage = taskKey[Unit]("Package the webpage")
val testPage = taskKey[Unit]("Build a test version of the webpage")

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
}

testPage := {
  // Depend on fastOptJS
  (fastOptJS in Compile).value

  val log = streams.value.log

  val baseTarget = s"${(target in Compile).value}/scala-2.11/"
  s"rm -rf $baseTarget/classes/webpage/target/" ! log
  s"mkdir -p $baseTarget/classes/webpage/target/" ! log
  file(s"$baseTarget/construct-fastopt.js") #> file(s"$baseTarget/classes/webpage/target/construct-fastopt.js") ! log
  file(s"$baseTarget/construct-jsdeps.js") #> file(s"$baseTarget/classes/webpage/target/construct-jsdeps.js") ! log
  Process("sed" :: "-i" :: "" :: "-e" :: "s%-opt.js%-fastopt.js%" :: s"$baseTarget/classes/webpage/index.html" :: Nil, Path.userHome) ! log
}
