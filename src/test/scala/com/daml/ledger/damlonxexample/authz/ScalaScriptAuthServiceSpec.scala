// Copyright (c) 2020 The DAML Authors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.daml.ledger.damlonxexample.authz

import java.io.StringReader

import com.digitalasset.ledger.api.auth.Claims
import io.grpc.Metadata
import org.scalatest._

class ScalaScriptAuthServiceSpec extends FlatSpec with Matchers {
  "A JavaScript authentication script" should "work correctly" in {
    val reader = new StringReader(
      """
        |import com.digitalasset.ledger.api.auth.Claims
        |import java.util.concurrent.CompletableFuture
        |import io.grpc.Metadata
        |
        |println(headers.asInstanceOf[Metadata].keys)
        |val res = new CompletableFuture[Claims]()
        |res.complete(Claims.wildcard)
        |res
        |""".stripMargin
    )
    val authzJS = new ScriptAuthService("scala", reader)
    authzJS.decodeMetadata(new Metadata()).toCompletableFuture.get() should be(Claims.wildcard)
  }
}