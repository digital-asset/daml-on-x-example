// Copyright (c) 2020 The DAML Authors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.daml.ledger.damlonxexample

import java.io.File

import com.daml.api.util.TimeProvider
import com.daml.ledger.api.auth.{AuthService, AuthServiceWildcard}
import com.daml.ledger.api.tls.TlsConfiguration
import com.daml.ledger.participant.state.v1.ParticipantId
import com.daml.ledger.participant.state.v1.SeedService.Seeding
import com.daml.platform.indexer.IndexerStartupMode
import com.daml.ports.Port
import org.apache.commons.io.FileUtils

final case class Config(
    port: Port,
    portFile: Option[File],
    archiveFiles: List[File],
    maxInboundMessageSize: Int,
    timeProvider: TimeProvider, // enables use of non-wall-clock time in tests
    address: Option[String],
    jdbcUrl: String,
    tlsConfig: Option[TlsConfiguration],
    participantId: ParticipantId,
    startupMode: IndexerStartupMode,
    authService: AuthService,
    seeding: Option[Seeding]
) {
  def withTlsConfig(modify: TlsConfiguration => TlsConfiguration): Config =
    copy(tlsConfig = Some(modify(tlsConfig.getOrElse(TlsConfiguration.Empty))))
}

object Config {
  val DefaultMaxInboundMessageSize: Int = 4 * FileUtils.ONE_MB.toInt

  def default: Config =
    new Config(
      port = Port(0),
      portFile = None,
      archiveFiles = List.empty,
      maxInboundMessageSize = DefaultMaxInboundMessageSize,
      timeProvider = TimeProvider.UTC,
      address = Some("0.0.0.0"),
      jdbcUrl = "",
      tlsConfig = None,
      participantId = ParticipantId.assertFromString("ephemeral-postgres-participant"),
      startupMode = IndexerStartupMode.MigrateAndStart,
      authService = AuthServiceWildcard,
      seeding = Some(Seeding.Weak)
    )
}
