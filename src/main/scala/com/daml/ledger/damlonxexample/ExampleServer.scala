// Copyright (c) 2020 The DAML Authors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.daml.ledger.damlonxexample

import java.io.File
import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.codahale.metrics.SharedMetricRegistries
import com.daml.ledger.participant.state.v1.{ReadService, SubmissionId, WriteService}
import com.digitalasset.daml.lf.archive.DarReader
import com.digitalasset.daml_lf_dev.DamlLf.Archive
import com.digitalasset.ledger.api.auth.{AuthService, AuthServiceWildcard}
import com.digitalasset.logging.LoggingContext
import com.digitalasset.logging.LoggingContext.newLoggingContext
import com.digitalasset.platform.apiserver.{ApiServerConfig, StandaloneApiServer}
import com.digitalasset.platform.indexer.{IndexerConfig, StandaloneIndexerServer}
import com.digitalasset.resources.akka.AkkaResourceOwner
import com.digitalasset.resources.{ProgramResource, ResourceOwner}
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
  implicit val materializer: Materializer = Materializer(system)

  val authService = AuthServiceWildcard

  val owner =
    for {
      // Take ownership of the actor system and materializer so they're cleaned up properly.
      // This is necessary because we can't declare them as implicits within a `for` comprehension.
      _ <- AkkaResourceOwner.forActorSystem(() => system)
      _ <- AkkaResourceOwner.forMaterializer(() => materializer)
      ledger <- ResourceOwner
        .forCloseable(() => new ExampleInMemoryParticipantState(config.participantId))
      _ <- ResourceOwner.forFuture(
        () => Future.sequence(config.archiveFiles.map(uploadDar(_, ledger)))
      )
      _ <- startParticipant(config, ledger)
      _ <- ResourceOwner.sequenceIgnoringValues(
        extraParticipantConfig(config).map(startParticipant(_, ledger))
      )
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

  private def extraParticipantConfig(base: Config): Vector[Config] =
    for ((extraParticipantId, port, jdbcUrl) <- base.extraParticipants)
      yield
        base.copy(
          port = port,
          participantId = extraParticipantId,
          jdbcUrl = jdbcUrl
        )

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
      SharedMetricRegistries.getOrCreate(s"indexer-${config.participantId}")
    )

  private def startApiServer(
      config: Config,
      readService: ReadService,
      writeService: WriteService,
      authService: AuthService
  )(implicit logCtx: LoggingContext): ResourceOwner[Unit] =
    new StandaloneApiServer(
      ApiServerConfig(
        config.participantId,
        config.archiveFiles,
        config.port,
        None,
        config.jdbcUrl,
        config.tlsConfig,
        config.timeProvider,
        config.maxInboundMessageSize,
        config.portFile.map(_.toPath)
      ),
      readService,
      writeService,
      authService,
      SharedMetricRegistries.getOrCreate(s"ledger-api-server-${config.participantId}")
    )
}
