// Copyright (c) 2020 The DAML Authors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.daml.ledger.damlonxexample

import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import com.digitalasset.daml.lf.archive.DarReader
import com.digitalasset.daml_lf_dev.DamlLf.Archive
import com.digitalasset.ledger.api.auth.AuthServiceWildcard
import com.digitalasset.platform.common.logging.NamedLoggerFactory
import com.codahale.metrics.SharedMetricRegistries
import com.daml.ledger.participant.state.v1.SubmissionId
import com.digitalasset.platform.apiserver.{ApiServerConfig, StandaloneApiServer}
import com.digitalasset.platform.indexer.{IndexerConfig, StandaloneIndexerServer}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scala.util.control.NonFatal

/** The example server is a fully compliant DAML Ledger API server
  * backed by the in-memory reference index and participant state implementations.
  * Not meant for production, or even development use cases, but for serving as a blueprint
  * for other implementations.
  */
object ExampleServer extends App with EphemeralPostgres {
  val logger = LoggerFactory.getLogger(this.getClass)

  val ephemeralPg = startEphemeralPg()

  val config = Cli
    .parse(
      args,
      "daml-on-x-example-server",
      "A fully compliant DAML Ledger API example in memory server"
    )
    .getOrElse(sys.exit(1))
    .copy(jdbcUrl = ephemeralPg.jdbcUrl)

  // Initialize Akka and log exceptions in flows.
  implicit val system: ActorSystem = ActorSystem("DamlonXExampleServer")
  implicit val ec: ExecutionContext = system.dispatcher
  implicit val materializer: ActorMaterializer = ActorMaterializer(
    ActorMaterializerSettings(system)
      .withSupervisionStrategy { e =>
        logger.error(s"Supervision caught exception: $e")
        Supervision.Stop
      }
  )

  val ledger = new ExampleInMemoryParticipantState(config.participantId)
  val readService = ledger
  val writeService = ledger
  val loggerFactory = NamedLoggerFactory.forParticipant(config.participantId)
  val authService = AuthServiceWildcard

  val participantF: Future[(AutoCloseable, AutoCloseable)] = for {
    indexer <- newIndexer(config)
    apiServer <- newApiServer(config).start()
  } yield (indexer, apiServer)

  private def newIndexer(config: Config) =
    StandaloneIndexerServer(
      readService,
      IndexerConfig(config.participantId, config.jdbcUrl, config.startupMode),
      NamedLoggerFactory.forParticipant(config.participantId),
      SharedMetricRegistries.getOrCreate(s"indexer-${config.participantId}")
    )

  private def newApiServer(config: Config) =
    new StandaloneApiServer(
      ApiServerConfig(
        config.participantId,
        config.archiveFiles,
        config.port,
        config.jdbcUrl,
        config.tlsConfig,
        config.timeProvider,
        config.maxInboundMessageSize,
        config.portFile
      ),
      readService,
      writeService,
      authService,
      NamedLoggerFactory.forParticipant(config.participantId),
      SharedMetricRegistries.getOrCreate(s"ledger-api-server-${config.participantId}")
    )

  private def archivesFromDar(file: File): List[Archive] = {
    DarReader[Archive] { case (_, x) => Try(Archive.parseFrom(x)) }
      .readArchiveFromFile(file)
      .fold(t => throw new RuntimeException(s"Failed to parse DAR from $file", t), dar => dar.all)
  }

  // Parse DAR archives given as command-line arguments and upload them
  // to the ledger using a side-channel.
  config.archiveFiles.foreach { f =>
    val submissionId = SubmissionId.assertFromString(UUID.randomUUID().toString)
    val archives = archivesFromDar(f)
    archives.foreach { archive =>
      logger.info(s"Uploading package ${archive.getHash}...")
    }
    ledger.uploadPackages(submissionId, archives, Some("uploaded on startup by participant"))
  }

  val closed = new AtomicBoolean(false)

  def closeServer(): Unit = {
    if (closed.compareAndSet(false, true)) {
      participantF.foreach {
        case (indexer, indexServer) =>
          indexer.close()
          indexServer.close()
      }
      ledger.close()
      materializer.shutdown()
      val _ = system.terminate()
    }
  }

  private def startupFailed(e: Throwable): Unit = {
    logger.error("Shutting down because of an initialization error.", e)
    closeServer()
    stopAndCleanUp(ephemeralPg.tempDir, ephemeralPg.dataDir, ephemeralPg.logFile)
  }

  participantF.failed.foreach(startupFailed)

  try {
    Runtime.getRuntime.addShutdownHook(new Thread(() => {
      closeServer()
      stopAndCleanUp(ephemeralPg.tempDir, ephemeralPg.dataDir, ephemeralPg.logFile)
    }))
  } catch {
    case NonFatal(e) =>
      startupFailed(e)
  }
}
