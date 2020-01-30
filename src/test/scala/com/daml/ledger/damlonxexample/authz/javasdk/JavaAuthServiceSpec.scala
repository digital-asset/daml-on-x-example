// Copyright (c) 2020 The DAML Authors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.daml.ledger.damlonxexample.authz.javasdk

import com.digitalasset.daml.lf.data.Ref
import io.grpc.Metadata
import org.scalatest.{FlatSpec, Matchers}
import com.digitalasset.ledger.api.auth.{
  ClaimActAsAnyParty => ScalaClaimActAsAnyParty,
  ClaimActAsParty => ScalaClaimActAsParty,
  ClaimAdmin => ScalaClaimAdmin,
  ClaimPublic => ScalaClaimPublic,
  ClaimReadAsParty => ScalaClaimReadAsParty,
  Claims => ScalaClaims
}

class JavaAuthServiceSpec extends FlatSpec with Matchers {

  "The empty Java `AuthService`" should "authorize as `ScalaClaims.empty`" in {
    new AuthServiceJavaAdapter(new EmptyJavaAuthService())
      .decodeMetadata(new Metadata())
      .toCompletableFuture
      .get() should be(
      ScalaClaims.empty
    )
  }

  "The wildcard Java `AuthService`" should "authorize as `ScalaClaims.wildcard`" in {
    new AuthServiceJavaAdapter(new WildcardJavaAuthService())
      .decodeMetadata(new Metadata())
      .toCompletableFuture
      .get() should be(ScalaClaims.wildcard)
  }

  "The non-trivial Java `AuthService`" should "authorize as corresponding Scala claims" in {
    new AuthServiceJavaAdapter(new NonTrivialJavaAuthService())
      .decodeMetadata(new Metadata())
      .toCompletableFuture
      .get() should be(
      new ScalaClaims(
        List(
          ScalaClaimAdmin,
          ScalaClaimPublic,
          ScalaClaimActAsAnyParty,
          ScalaClaimReadAsParty(Ref.Party.assertFromString("P")),
          ScalaClaimActAsParty(Ref.Party.assertFromString("P"))
        ),
        None,
        None,
        None
      )
    )
  }
}
