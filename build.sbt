
import pl.project13.scala.sbt.JmhPlugin

name := "scala-language-tests"

version := "1.0"

/** Reference version, shows current performance. Change this value to name of Scala
  *  version with reflection cache sync changes. Then move to project folder and
  *  execute "sbt clean compile jmh:run" */
val scalaVersionString = "2.12.0-RC1"

//val scalaVersionString = "2.12.0-SNAPSHOT"
//val scalaVersionString = "2.12.0-SNAPSHOT-P"

scalaVersion := scalaVersionString

scalaVersion in ThisBuild := scalaVersionString

scalaBinaryVersion := scalaVersionString

libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersionString

enablePlugins(JmhPlugin)

sourceDirectory in Jmh := (sourceDirectory in Test).value
classDirectory in Jmh := (classDirectory in Test).value
dependencyClasspath in Jmh := (dependencyClasspath in Test).value
// rewire tasks, so that 'jmh:run' automatically invokes 'jmh:compile' (otherwise a clean 'jmh:run' would fail)
compile in Jmh <<= (compile in Jmh) dependsOn (compile in Test)
run in Jmh <<= (run in Jmh) dependsOn (Keys.compile in Jmh)