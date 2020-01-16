// Copyright (c) 2020 The DAML Authors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.daml.ledger.damlonxexample

import java.io.File

import com.daml.ledger.damlonxexample.authz.JarFileAuthService
import com.digitalasset.ledger.api.auth.Claims
import io.grpc.Metadata
import org.scalatest._

class AuthPluginSpec extends FlatSpec with Matchers {
  "The authentication plugin JAR mechanism" should "work correctly" in {
    val authzJarPath = System.getProperty("authz.jar")
    val authzClassName = System.getProperty("authz.class")
    val authzExternalImpl = new JarFileAuthService(new File(authzJarPath), authzClassName)

    authzExternalImpl.decodeMetadata(new Metadata()).toCompletableFuture.get() should be(Claims.wildcard)
  }
}