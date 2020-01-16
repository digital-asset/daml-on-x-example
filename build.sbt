import Dependencies._
import Classes._

ThisBuild / scalaVersion := "2.12.8"
ThisBuild / version := "0.1.3-SNAPSHOT"
ThisBuild / organization := "com.daml"
ThisBuild / organizationName := "Digital Asset, LLC"

lazy val sdkVersion = "100.13.41"
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

lazy val testAuthzPlugin = (project in file("./test-authz-plugin"))
  .settings(
    name := "DAML-on-X Test Authorization Plugin",
    libraryDependencies ++= Seq(
      "com.digitalasset.ledger" %% "ledger-api-auth" % sdkVersion,
    ),
    exportJars := true,
    artifactName := { (_: ScalaVersion, _: ModuleID, _: Artifact) =>
      "daml-on-x-test-authz-plugin.jar"
    }
  )

lazy val authPluginPathExt = settingKey[String]("Get the authz-pluguin artifact path")
authPluginPathExt := (artifactPath in (Compile, packageBin) in testAuthzPlugin).value.getPath

lazy val root = (project in file("."))
  .settings(
    name := "DAML-on-X Example Ledger Implementation",
    libraryDependencies ++= Seq(
      scalaTest % Test,
      "com.digitalasset" % "daml-lf-dev-archive-java-proto" % sdkVersion,
      "com.digitalasset" %% "daml-lf-data" % sdkVersion,
      "com.digitalasset" %% "daml-lf-engine" % sdkVersion,
      "com.digitalasset" %% "daml-lf-language" % sdkVersion,

      "com.digitalasset.platform" %% "sandbox" % sdkVersion,
      "com.digitalasset.ledger" %% "ledger-api-auth" % sdkVersion,

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
    fork in Test := true,
    javaOptions in Test ++= Seq(s"-Dauthz.jar=${authPluginPathExt.value}", s"-Dauthz.class=$authzPluginClass"),
    resolvers += "Digital Asset SDK".at("https://digitalassetsdk.bintray.com/DigitalAssetSDK"),
  )
  .dependsOn(testAuthzPlugin % Test)