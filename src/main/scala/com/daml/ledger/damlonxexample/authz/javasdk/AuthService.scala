// Copyright (c) 2020 The DAML Authors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.daml.ledger.damlonxexample.authz.javasdk

import java.util.concurrent.CompletionStage

import io.grpc.Metadata

/** Mirrors [[com.digitalasset.ledger.api.auth.AuthService]] */

trait AuthService {
  def decodeMetadata(headers: Metadata): CompletionStage[Claims]
}
