import play.sbt.PlayImport._
import sbt._

object Dependencies {

  val slf4jVersion = "1.6.4"
  val slf4jNop = "org.slf4j" % "slf4j-nop" % slf4jVersion

  val akkaVersion = "2.4.17"

  val commonDependencies: Seq[ModuleID] = Seq(
    "ch.qos.logback" % "logback-classic" % "1.1.3",
    "org.slf4j" % "log4j-over-slf4j" % "1.7.12",
    "com.google.inject" % "guice" % "4.1.0",
    "org.scalatest" %% "scalatest" % "2.2.4" % "test",
    slf4jNop,
    "org.threeten" % "threetenbp" % "1.3",
    "junit" % "junit" % "4.12" % "test"
  )

  val json: Seq[ModuleID] = Seq(
    "io.argonaut" %% "argonaut" % "6.0.4",
    "com.propensive" %% "rapture-json-argonaut" % "1.1.0",
    "com.typesafe.play" %% "play-json" % "2.5.12")


  val transformerDependencies: Seq[ModuleID] = commonDependencies ++ Seq(
    "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
    "com.typesafe.akka" %% "akka-distributed-data-experimental" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream-kafka" % "0.13",
    "com.twitter" %% "algebird-core" % "0.13.0",
    "com.trueaccord.scalapb" %% "compilerplugin" % "0.6.0-pre1",
    "net.manub" %% "scalatest-embedded-kafka" % "0.7.1"
      exclude("log4j", "log4j"),
    "com.typesafe.slick" %% "slick" % "3.0.0",
    "com.h2database" % "h2" % "1.4.193",
    "net.cakesolutions" %% "scala-kafka-client" % "0.10.2.0",
    "net.cakesolutions" %% "scala-kafka-client-testkit" % "0.10.2.0" % Test,
    "org.mockito" % "mockito-core" % "2.7.17" % Test,
    specs2 % Test
  )

  val webDependencies: Seq[ModuleID] = commonDependencies ++ json ++ {
    Seq(
      "net.cakesolutions" %% "scala-kafka-client" % "0.10.2.0",
      "net.cakesolutions" %% "scala-kafka-client-akka" % "0.10.2.0",
      "net.cakesolutions" %% "scala-kafka-client-testkit" % "0.10.2.0" % Test,
      "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0" % Test
    )
  }

}
