// Copyright (c) 2020 The DAML Authors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.daml.ledger.damlonxexample.authz

import java.io.Reader
import java.util.concurrent.{CompletableFuture, CompletionStage}

import com.digitalasset.ledger.api.auth.{AuthService, Claims}
import io.grpc.Metadata
import javax.script.{ScriptEngine, ScriptEngineManager}

class ScriptAuthService(val engineName: String, private val reader: Reader) extends AuthService {

  private val engine: ScriptEngine = {
    val manager = new ScriptEngineManager()
    manager.getEngineByName(engineName)
  }

  override def decodeMetadata(headers: Metadata): CompletionStage[Claims] = {
    engine.put("headers", headers)
    engine.eval(reader).asInstanceOf[CompletableFuture[Claims]]
  }
}
