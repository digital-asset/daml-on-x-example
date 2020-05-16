// Copyright (c) 2020 The DAML Authors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.daml.ledger.damlonxexample

import java.io.File
import java.time.Duration
import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.codahale.metrics.SharedMetricRegistries
import com.daml.daml_lf_dev.DamlLf.Archive
import com.daml.ledger.api.auth.{AuthService, AuthServiceWildcard}
import com.daml.ledger.participant.state.v1._
import com.daml.lf.archive.DarReader
import com.daml.lf.engine.Engine
import com.daml.logging.LoggingContext
import com.daml.logging.LoggingContext.newLoggingContext
import com.daml.metrics.Metrics
import com.daml.platform.apiserver.{
  ApiServer,
  ApiServerConfig,
  StandaloneApiServer,
  TimedIndexService
}
import com.daml.platform.configuration.{
  CommandConfiguration,
  LedgerConfiguration,
  PartyConfiguration
}
import com.daml.platform.indexer.{IndexerConfig, StandaloneIndexerServer}
import com.daml.resources.akka.AkkaResourceOwner
import com.daml.resources.{ProgramResource, ResourceOwner}
import org.slf4j.LoggerFactory

import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

/** The example server is a fully compliant DAML Ledger API server
  * backed by the in-memory reference index and participant state implementations.
  * Not meant for production, or even development use cases, but for serving as a blueprint
  * for other implementations.
  */
object ExampleServer extends App with EphemeralPostgres {
  val logger = LoggerFactory.getLogger(this.getClass)

  val defaultConfig = Cli
    .parse(
      args,
      "daml-on-x-example-server",
      "A fully compliant DAML Ledger API example in memory server"
    )
    .getOrElse(sys.exit(1))

  var config = defaultConfig

  if (defaultConfig.jdbcUrl contains ("h2")) {
    config = defaultConfig
  } else {
    val ephemeralPg = startEphemeralPg()
    println(ephemeralPg.jdbcUrl)
    config = defaultConfig.copy(jdbcUrl = ephemeralPg.jdbcUrl)
  }

  val sharedEngine = Engine()

  private val metricsRegistry =
    SharedMetricRegistries.getOrCreate(s"ledger-api-server-${config.participantId}")
  private val metrics = new Metrics(metricsRegistry)

  // Initialize Akka and log exceptions in flows.
  implicit val system: ActorSystem = ActorSystem("DamlonXExampleServer")
  implicit val ec: ExecutionContext = system.dispatcher
  implicit val materializer: Materializer = Materializer(system)

  val authService = AuthServiceWildcard

  val owner =
    for {
      // Take ownership of the actor system and materializer so they're cleaned up properly.
      // This is necessary because we can't declare them as implicits within a `for` comprehension.
      _ <- AkkaResourceOwner.forActorSystem(() => system)
      _ <- AkkaResourceOwner.forMaterializer(() => materializer)

      ledger <- ResourceOwner
        .forCloseable(
          () =>
            new ExampleInMemoryParticipantState(
              participantId = config.participantId,
              metrics = metrics,
              engine = sharedEngine
            )
        )
      _ <- ResourceOwner.forFuture(
        () => Future.sequence(config.archiveFiles.map(uploadDar(_, ledger)))
      )
      _ <- startParticipant(config, ledger)
    } yield ()

  new ProgramResource(owner).run()

  private def uploadDar(from: File, to: ExampleInMemoryParticipantState): Future[Unit] = {
    val submissionId = SubmissionId.assertFromString(UUID.randomUUID().toString)
    for {
      dar <- Future(
        DarReader { case (_, x) => Try(Archive.parseFrom(x)) }.readArchiveFromFile(from).get
      )
      _ <- to.uploadPackages(submissionId, dar.all, None).toScala
    } yield ()
  }

  private def startParticipant(
      config: Config,
      ledger: ExampleInMemoryParticipantState
  ): ResourceOwner[Unit] =
    newLoggingContext { implicit logCtx =>
      for {
        _ <- startIndexerServer(config, readService = ledger)
        _ <- startApiServer(
          config,
          readService = ledger,
          writeService = ledger,
          authService = AuthServiceWildcard
        )
      } yield ()
    }

  private def startIndexerServer(config: Config, readService: ReadService)(
      implicit logCtx: LoggingContext
  ): ResourceOwner[Unit] =
    new StandaloneIndexerServer(
      readService,
      IndexerConfig(config.participantId, config.jdbcUrl, config.startupMode),
      metrics
    )

  private def startApiServer(
      config: Config,
      readService: ReadService,
      writeService: WriteService,
      authService: AuthService
  )(implicit logCtx: LoggingContext): ResourceOwner[ApiServer] =
    new StandaloneApiServer(
      ApiServerConfig(
        participantId = config.participantId,
        archiveFiles = config.archiveFiles,
        port = config.port,
        address = config.address,
        jdbcUrl = config.jdbcUrl,
        tlsConfig = config.tlsConfig,
        maxInboundMessageSize = config.maxInboundMessageSize,
        eventsPageSize = 10,
        portFile = config.portFile.map(_.toPath),
        seeding = config.seeding
      ),
      commandConfig = CommandConfiguration.default,
      partyConfig = PartyConfiguration(false),
      ledgerConfig = LedgerConfiguration(
        initialConfiguration = Configuration(
          generation = 1,
          timeModel = TimeModel(
            avgTransactionLatency = Duration.ofSeconds(0L),
            minSkew = Duration.ofMinutes(2),
            //TODO BH: temporarily give big tolerance on time while ensuring proper way of ticking it
            maxSkew = Duration.ofMinutes(2)
          ).get,
          maxDeduplicationTime = Duration.ofDays(1)
        ),
        initialConfigurationSubmitDelay = Duration.ofSeconds(5),
        configurationLoadTimeout = Duration.ofSeconds(30)
      ),
      readService,
      writeService,
      authService,
      transformIndexService = service =>
        new TimedIndexService(
          service,
          metrics
        ),
      metrics,
      engine = sharedEngine
    )
}
