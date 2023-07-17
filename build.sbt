name := "akka-essentials-typed"

version := "0.1"

scalaVersion := "2.12.15"

val akkaVersion = "2.6.17"
val scalaTestVersion = "3.2.2"
val logbackVersion = "1.2.5"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion,
  "org.scalatest" %% "scalatest" % scalaTestVersion,
  "ch.qos.logback" % "logback-classic" % logbackVersion,
)

import sbt._
import sbt.Credentials

credentials += Credentials(Path.userHome / ".sbt" / ".credentials")