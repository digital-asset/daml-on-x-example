import Dependencies._

ThisBuild / scalaVersion := "2.12.8"
ThisBuild / version := "0.1.3-SNAPSHOT"
ThisBuild / organization := "com.daml"
ThisBuild / organizationName := "Digital Asset, LLC"

lazy val sdkVersion = "1.2.0-snapshot.20200513.4172.0.021f4af3"
lazy val akkaVersion = "2.6.1"

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
  case "META-INF/versions/9/module-info.class" => MergeStrategy.first
  case PathList("google", "protobuf", n) if n endsWith ".proto" =>
    // Both in protobuf and akka
    MergeStrategy.first
  case "module-info.class" =>
    // In all 2.10 Jackson JARs
    MergeStrategy.discard
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
      "com.daml" % "daml-lf-dev-archive-java-proto" % sdkVersion,
      "com.daml" %% "contextualized-logging" % sdkVersion,
      "com.daml" %% "daml-lf-archive-reader" % sdkVersion,
      "com.daml" %% "daml-lf-data" % sdkVersion,
      "com.daml" %% "daml-lf-engine" % sdkVersion,
      "com.daml" %% "daml-lf-language" % sdkVersion,
      "com.daml" %% "daml-lf-transaction" % sdkVersion,

      "com.daml" %% "sandbox" % sdkVersion,
      "com.daml" %% "ledger-api-auth" % sdkVersion,

      "com.daml" %% "participant-state" % sdkVersion ,
      "com.daml" %% "participant-state-kvutils" % sdkVersion,
      "com.daml" %% "participant-state-kvutils-app" % sdkVersion,

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
    )
  )
