import Dependencies._

ThisBuild / scalaVersion := "2.12.8"
ThisBuild / version := "0.1.3-SNAPSHOT"
ThisBuild / organization := "com.daml"
ThisBuild / organizationName := "Digital Asset, LLC"

lazy val sdkVersion = "100.13.38"
lazy val akkaVersion = "2.5.23"
lazy val jacksonVersion = "2.9.8"

// This task is used by the integration test to detect which version of Ledger API Test Tool to use.
val printSdkVersion = taskKey[Unit]("printSdkVersion")
printSdkVersion := println(sdkVersion)

assemblyMergeStrategy in assembly := {
  case "META-INF/io.netty.versions.properties" =>
    // Looks like multiple versions patch versions of of io.netty are getting
    // into dependency graph, choose one.
    MergeStrategy.first
  case PathList(ps @ _*) if ps.last startsWith "com/fasterxml/jackson" =>
    MergeStrategy.first
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}
assemblyJarName in assembly := "damlonx-example.jar"

lazy val root = (project in file("."))
  .settings(
    name := "DAML-on-X Example Ledger Implementation",
    libraryDependencies ++= Seq(
      scalaTest % Test,
      "com.digitalasset" % "daml-lf-dev-archive-java-proto" % sdkVersion,
      "com.digitalasset" %% "daml-lf-data" % sdkVersion,
      "com.digitalasset" %% "daml-lf-engine" % sdkVersion,
      "com.digitalasset" %% "daml-lf-language" % sdkVersion,

      "com.digitalasset.platform" %% "sandbox" % sdkVersion withExclusions(Vector(
        ExclusionRule("com.fasterxml.jackson.core")
      )),
       "com.digitalasset.ledger" %% "ledger-api-auth" % sdkVersion withExclusions(Vector(
         ExclusionRule("com.fasterxml.jackson.core")
       )),

      "com.daml.ledger" %% "participant-state" % sdkVersion ,
      "com.daml.ledger" %% "participant-state-kvutils" % sdkVersion,

      "com.typesafe.akka" %% "akka-actor" % akkaVersion,
      "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
      "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,

      "org.slf4j" % "slf4j-api" % "1.7.26",
      "ch.qos.logback" % "logback-core" % "1.2.3",
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "commons-io" % "commons-io" % "2.6",
      "com.github.scopt" %% "scopt" % "4.0.0-RC2",

    ),
    resolvers += "Digital Asset SDK".at("https://digitalassetsdk.bintray.com/DigitalAssetSDK"),
  )
