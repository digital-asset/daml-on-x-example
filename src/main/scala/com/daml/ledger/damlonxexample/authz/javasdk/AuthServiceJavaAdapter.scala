// Copyright (c) 2020 The DAML Authors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.daml.ledger.damlonxexample.authz.javasdk

import java.util.concurrent.CompletionStage

import com.digitalasset.ledger.api.auth.{AuthService => ScalaAuthService, Claims => ScalaClaims}
import io.grpc.Metadata

final class AuthServiceJavaAdapter(private val authService: AuthService) extends ScalaAuthService {
  def decodeMetadata(headers: Metadata): CompletionStage[ScalaClaims] =
    authService
      .decodeMetadata(headers)
      .thenApply { javaClaims =>
        import Claims.ToScala._
        javaClaims
      }
}
