// Copyright (c) 2020 The DAML Authors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.daml.ledger.damlonxexample.authz

import java.time.Instant
import java.util.concurrent.{CompletableFuture, CompletionStage}

import com.digitalasset.daml.lf.data.Ref
import com.digitalasset.ledger.api.auth.{AuthService, ClaimActAsParty, Claims}
import io.grpc.Metadata
import org.scalatest._

class ChainAuthServiceSpec extends FlatSpec with Matchers {
  private val canActAsP = Claims(Vector(ClaimActAsParty(Ref.Party.assertFromString("P"))))

  "The intersection authorization of wildcard and empty" should "be empty" in {
    val authz1 = new AuthService() {
      override def decodeMetadata(headers: Metadata): CompletionStage[Claims] =
        CompletableFuture.completedFuture(Claims.wildcard)
    }

    val authz2 = new AuthService() {
      override def decodeMetadata(headers: Metadata): CompletionStage[Claims] =
        CompletableFuture.completedFuture(Claims.empty)
    }

    val chainIntersectAuthz = new ChainAuthService(List(authz1, authz2))

    chainIntersectAuthz.decodeMetadata(new Metadata()).toCompletableFuture.get() should be(
      Claims.empty
    )
  }

  "The union authorization of wildcard and empty" should "be wildcard" in {
    val authz1 = new AuthService() {
      override def decodeMetadata(headers: Metadata): CompletionStage[Claims] =
        CompletableFuture.completedFuture(Claims.wildcard)
    }

    val authz2 = new AuthService() {
      override def decodeMetadata(headers: Metadata): CompletionStage[Claims] =
        CompletableFuture.completedFuture(Claims.empty)
    }

    val chainIntersectAuthz = new ChainAuthService(List(authz1, authz2), false)

    chainIntersectAuthz
      .decodeMetadata(new Metadata())
      .toCompletableFuture
      .get()
      .claims
      .toSet should be(Claims.wildcard.claims.toSet)
  }

  "The intersection authorization of wildcard and a party" should "be a party" in {

    val authz1 = new AuthService() {
      override def decodeMetadata(headers: Metadata): CompletionStage[Claims] =
        CompletableFuture.completedFuture(Claims.wildcard)
    }

    val authz2 = new AuthService() {
      override def decodeMetadata(headers: Metadata): CompletionStage[Claims] = {
        CompletableFuture.completedFuture(canActAsP)
      }
    }

    val chainIntersectAuthz = new ChainAuthService(List(authz1, authz2))

    chainIntersectAuthz.decodeMetadata(new Metadata()).toCompletableFuture.get() should be(
      canActAsP
    )
  }

  "The union authorization of wildcard and a party" should "be wildcard" in {
    val authz1 = new AuthService() {
      override def decodeMetadata(headers: Metadata): CompletionStage[Claims] =
        CompletableFuture.completedFuture(Claims.wildcard)
    }

    val authz2 = new AuthService() {
      override def decodeMetadata(headers: Metadata): CompletionStage[Claims] = {
        CompletableFuture.completedFuture(canActAsP)
      }
    }

    val chainIntersectAuthz = new ChainAuthService(List(authz1, authz2), false)

    chainIntersectAuthz
      .decodeMetadata(new Metadata())
      .toCompletableFuture
      .get()
      .claims
      .toSet should be(Claims.wildcard.claims.toSet)
  }

  private val canActAsPOnLedgerL = canActAsP.copy(ledgerId = Some("L"))

  "The intersection authorization of ledgerId and unrestricted" should "be ledgerId" in {
    val authz1 = new AuthService() {
      override def decodeMetadata(headers: Metadata): CompletionStage[Claims] =
        CompletableFuture.completedFuture(canActAsPOnLedgerL)
    }

    val authz2 = new AuthService() {
      override def decodeMetadata(headers: Metadata): CompletionStage[Claims] =
        CompletableFuture.completedFuture(canActAsP)
    }

    val chainIntersectAuthz = new ChainAuthService(List(authz1, authz2))

    chainIntersectAuthz.decodeMetadata(new Metadata()).toCompletableFuture.get() should be(
      canActAsPOnLedgerL
    )
  }

  "The union authorization of ledgerId and unrestricted" should "be unrestricted" in {
    val authz1 = new AuthService() {
      override def decodeMetadata(headers: Metadata): CompletionStage[Claims] =
        CompletableFuture.completedFuture(canActAsPOnLedgerL)
    }

    val authz2 = new AuthService() {
      override def decodeMetadata(headers: Metadata): CompletionStage[Claims] =
        CompletableFuture.completedFuture(canActAsP)
    }

    val chainIntersectAuthz = new ChainAuthService(List(authz1, authz2), false)

    chainIntersectAuthz.decodeMetadata(new Metadata()).toCompletableFuture.get() should be(
      canActAsP
    )
  }

  private val canActAsPOnLedgerL1 = canActAsP.copy(ledgerId = Some("L1"))
  private val canActAsPOnLedgerL2 = canActAsP.copy(ledgerId = Some("L2"))

  "The intersection authorization of ledgerId1 and ledgerId2" should "be empty" in {
    val authz1 = new AuthService() {
      override def decodeMetadata(headers: Metadata): CompletionStage[Claims] =
        CompletableFuture.completedFuture(canActAsPOnLedgerL1)
    }

    val authz2 = new AuthService() {
      override def decodeMetadata(headers: Metadata): CompletionStage[Claims] =
        CompletableFuture.completedFuture(canActAsPOnLedgerL2)
    }

    val chainIntersectAuthz = new ChainAuthService(List(authz1, authz2))

    chainIntersectAuthz.decodeMetadata(new Metadata()).toCompletableFuture.get() should be(
      Claims.empty
    )
  }

  "The union authorization of ledgerId1 and ledgerId2" should "be empty" in {
    val authz1 = new AuthService() {
      override def decodeMetadata(headers: Metadata): CompletionStage[Claims] =
        CompletableFuture.completedFuture(canActAsPOnLedgerL1)
    }

    val authz2 = new AuthService() {
      override def decodeMetadata(headers: Metadata): CompletionStage[Claims] =
        CompletableFuture.completedFuture(canActAsPOnLedgerL2)
    }

    val chainIntersectAuthz = new ChainAuthService(List(authz1, authz2), false)

    chainIntersectAuthz.decodeMetadata(new Metadata()).toCompletableFuture.get() should be(
      Claims.empty
    )
  }

  private val canActAsPOnParticipantP = canActAsP.copy(participantId = Some("P"))

  "The intersection authorization of participantId and unrestricted" should "be participantId" in {
    val authz1 = new AuthService() {
      override def decodeMetadata(headers: Metadata): CompletionStage[Claims] =
        CompletableFuture.completedFuture(canActAsPOnParticipantP)
    }

    val authz2 = new AuthService() {
      override def decodeMetadata(headers: Metadata): CompletionStage[Claims] =
        CompletableFuture.completedFuture(canActAsP)
    }

    val chainIntersectAuthz = new ChainAuthService(List(authz1, authz2))

    chainIntersectAuthz.decodeMetadata(new Metadata()).toCompletableFuture.get() should be(
      canActAsPOnParticipantP
    )
  }

  "The union authorization of participantId and unrestricted" should "be unrestricted" in {
    val authz1 = new AuthService() {
      override def decodeMetadata(headers: Metadata): CompletionStage[Claims] =
        CompletableFuture.completedFuture(canActAsPOnParticipantP)
    }

    val authz2 = new AuthService() {
      override def decodeMetadata(headers: Metadata): CompletionStage[Claims] =
        CompletableFuture.completedFuture(canActAsP)
    }

    val chainIntersectAuthz = new ChainAuthService(List(authz1, authz2), false)

    chainIntersectAuthz.decodeMetadata(new Metadata()).toCompletableFuture.get() should be(
      canActAsP
    )
  }

  private val canActAsPUntil = canActAsP.copy(expiration = Some(Instant.MAX))

  "The intersection authorization until some time and unrestricted" should "be until some time" in {
    val authz1 = new AuthService() {
      override def decodeMetadata(headers: Metadata): CompletionStage[Claims] =
        CompletableFuture.completedFuture(canActAsPUntil)
    }

    val authz2 = new AuthService() {
      override def decodeMetadata(headers: Metadata): CompletionStage[Claims] =
        CompletableFuture.completedFuture(canActAsP)
    }

    val chainIntersectAuthz = new ChainAuthService(List(authz1, authz2))

    chainIntersectAuthz.decodeMetadata(new Metadata()).toCompletableFuture.get() should be(
      canActAsPUntil
    )
  }

  "The union authorization of until some time and unrestricted" should "be unrestricted" in {
    val authz1 = new AuthService() {
      override def decodeMetadata(headers: Metadata): CompletionStage[Claims] =
        CompletableFuture.completedFuture(canActAsPUntil)
    }

    val authz2 = new AuthService() {
      override def decodeMetadata(headers: Metadata): CompletionStage[Claims] =
        CompletableFuture.completedFuture(canActAsP)
    }

    val chainIntersectAuthz = new ChainAuthService(List(authz1, authz2), false)

    chainIntersectAuthz.decodeMetadata(new Metadata()).toCompletableFuture.get() should be(
      canActAsP
    )
  }
}
