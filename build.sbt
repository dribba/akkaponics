name := """akkaponics"""

version := "0.0.1"

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.11",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.11" % "test",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test",
  "org.mockito" % "mockito-core" % "1.10.19" % "test",
  "com.pi4j" % "pi4j-core" % "1.0"
)

organization := "me.dribba"

assemblyJarName in assembly := name.value + "-" + version.value + ".jar"

// Skip test in assembly
test in assembly := {}