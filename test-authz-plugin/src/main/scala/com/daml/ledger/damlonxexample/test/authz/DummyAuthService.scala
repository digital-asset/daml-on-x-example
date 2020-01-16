// Copyright (c) 2020 The DAML Authors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.daml.ledger.damlonxexample.test.authz

import java.util.concurrent.{CompletableFuture, CompletionStage}

import com.digitalasset.ledger.api.auth.{AuthService, Claims}
import io.grpc.Metadata

class DummyAuthService extends AuthService {
  override def decodeMetadata(headers: Metadata): CompletionStage[Claims] =
    CompletableFuture.completedStage(Claims.wildcard)
}
