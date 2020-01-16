// Copyright (c) 2020 The DAML Authors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.daml.ledger.damlonxexample.authz

import java.io.File
import java.net.URLClassLoader
import java.util.concurrent.CompletionStage

import com.digitalasset.ledger.api.auth.{AuthService, Claims}
import io.grpc.Metadata

class JarFileAuthService(private val jarFile: File, private val className: String) extends AuthService {

  private val delegate: AuthService =
    URLClassLoader
      .newInstance(Array(jarFile.toURI.toURL))
      .loadClass(className)
      .getDeclaredConstructor()
      .newInstance()
      .asInstanceOf[AuthService]

  override def decodeMetadata(headers: Metadata): CompletionStage[Claims] =
    delegate.decodeMetadata(headers)
}