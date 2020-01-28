// Copyright (c) 2020 The DAML Authors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.daml.ledger.damlonxexample.authz

import java.time.Instant
import java.util.concurrent.{CompletableFuture, CompletionStage}

import com.daml.ledger.damlonxexample.authz.ChainAuthzMode.ChainzAuthMode
import com.digitalasset.daml.lf.data.Ref
import com.digitalasset.ledger.api.auth.{AuthService, ClaimActAsParty, Claims}
import io.grpc.Metadata
import org.scalatest._

class ChainAuthServiceSpec extends FlatSpec with Matchers {
  private def authServiceReturning(claims: Claims) = new AuthService {
    override def decodeMetadata(headers: Metadata): CompletionStage[Claims] =
      CompletableFuture.completedFuture(claims)
  }

  private def chainAuthz(
      authzs: Seq[AuthService],
      intersect: ChainzAuthMode = ChainAuthzMode.Intersection
  ) = {
    new ChainAuthService(authzs, intersect)
      .decodeMetadata(new Metadata())
      .toCompletableFuture
      .get()
  }

  "The intersection authorization of nothing" should "be empty" in {
    chainAuthz(List.empty) should be(
      Claims.empty
    )
  }

  "The union authorization of nothing" should "be empty" in {
    chainAuthz(List.empty, ChainAuthzMode.Union) should be(
      Claims.empty
    )
  }

  private val authzEmpty = authServiceReturning(Claims.empty)
  private val authzWildcard = authServiceReturning(Claims.wildcard)

  "The intersection authorization of wildcard and empty" should "be empty" in {
    chainAuthz(List(authzEmpty, authzWildcard)) should be(
      Claims.empty
    )
  }

  "The union authorization of wildcard and empty" should "be wildcard" in {
    chainAuthz(List(authzEmpty, authzWildcard), ChainAuthzMode.Union).claims.toSet should be(
      Claims.wildcard.claims.toSet
    )
  }

  private val canActAsPClaim = ClaimActAsParty(Ref.Party.assertFromString("P"))
  private val canActAsPUnrestricted = Claims(
    Vector(canActAsPClaim)
  )
  private val authzPUnrestricted = authServiceReturning(canActAsPUnrestricted)

  "The intersection authorization of wildcard and a party P" should "be a party P" in {
    chainAuthz(List(authzWildcard, authzPUnrestricted)) should be(
      canActAsPUnrestricted
    )
  }

  "The union authorization of wildcard and a party P" should "be wildcard" in {
    chainAuthz(List(authzWildcard, authzPUnrestricted), ChainAuthzMode.Union).claims.toSet should be(
      Claims.wildcard.claims.toSet
    )
  }

  "The intersection authorization of unrestricted party P and unrestricted party P" should "be unrestricted party P" in {
    chainAuthz(List(authzPUnrestricted, authzPUnrestricted)) should be(
      canActAsPUnrestricted
    )
  }

  "The union authorization of unrestricted party P and unrestricted party P" should "be unrestricted party P " in {
    chainAuthz(List(authzPUnrestricted, authzPUnrestricted), ChainAuthzMode.Union) should be(
      canActAsPUnrestricted
    )
  }

  private val canActAsPTickClaim = ClaimActAsParty(Ref.Party.assertFromString("PTick"))
  private val canActAsPTickUnrestricted = Claims(
    Vector(canActAsPTickClaim)
  )
  private val authzPTickUnrestricted = authServiceReturning(canActAsPTickUnrestricted)

  "The intersection authorization of unrestricted party P and unrestricted party P'" should "be empty" in {
    chainAuthz(List(authzPUnrestricted, authzPTickUnrestricted)) should be(
      Claims.empty
    )
  }

  "The union authorization of unrestricted party P and unrestricted party P'" should "be unrestricted party P and P'" in {
    chainAuthz(List(authzPUnrestricted, authzPTickUnrestricted), ChainAuthzMode.Union) should be(
      Claims(
        Vector(canActAsPClaim, canActAsPTickClaim)
      )
    )
  }

  private val canActAsPOnLedgerL = canActAsPUnrestricted.copy(ledgerId = Some("L"))
  private val authzPOnLedgerL = authServiceReturning(canActAsPOnLedgerL)

  "The intersection authorization of ledger-L-restricted party P and unrestricted" should "be ledger-L-restricted party P" in {
    chainAuthz(List(authzPOnLedgerL, authzPUnrestricted)) should be(
      canActAsPOnLedgerL
    )
  }

  "The union authorization of ledger-L-restricted party P and unrestricted party P" should "be unrestricted party P" in {
    chainAuthz(List(authzPOnLedgerL, authzPUnrestricted), ChainAuthzMode.Union) should be(
      canActAsPUnrestricted
    )
  }

  private val canActAsPOnLedgerLTick = canActAsPUnrestricted.copy(ledgerId = Some("L'"))
  private val authzPOnLedgerLTick = authServiceReturning(canActAsPOnLedgerLTick)

  "The intersection authorization of ledger-L-restricted party P and ledger-L'-restricted party P" should "be empty" in {
    chainAuthz(List(authzPOnLedgerL, authzPOnLedgerLTick)) should be(
      Claims.empty
    )
  }

  "The union authorization of ledger-L-restricted party P and ledger-L'-restricted party P" should "be empty" in {
    chainAuthz(List(authzPOnLedgerL, authzPOnLedgerLTick), ChainAuthzMode.Union) should be(
      Claims.empty
    )
  }

  private val canActAsPOnParticipantP = canActAsPUnrestricted.copy(participantId = Some("P"))
  private val authzPOnParticipantP = authServiceReturning(canActAsPOnParticipantP)

  "The intersection authorization of participantId-P-restricted party P and unrestricted party P" should "be participantId-P-restricted party P" in {
    chainAuthz(List(authzPOnParticipantP, authzPUnrestricted)) should be(
      canActAsPOnParticipantP
    )
  }

  "The union authorization of participantId-P-restricted party P and unrestricted party P" should "be unrestricted party P" in {
    chainAuthz(List(authzPOnParticipantP, authzPUnrestricted), ChainAuthzMode.Union) should be(
      canActAsPUnrestricted
    )
  }

  private val canActAsPOnParticipantPTick = canActAsPUnrestricted.copy(participantId = Some("P'"))
  private val authzPOnParticipantPTick = authServiceReturning(canActAsPOnParticipantPTick)

  "The intersection authorization of participant-P-restricted party P and participant-P'-restricted party P" should "be empty" in {
    chainAuthz(List(authzPOnParticipantP, authzPOnParticipantPTick)) should be(
      Claims.empty
    )
  }

  "The union authorization of participant-P-restricted party P and participant-P'-restricted party P" should "be empty" in {
    chainAuthz(List(authzPOnParticipantP, authzPOnParticipantPTick), ChainAuthzMode.Union) should be(
      Claims.empty
    )
  }

  private val canActAsPUntilI = canActAsPUnrestricted.copy(expiration = Some(Instant.MAX))
  private val authzPUntilI = authServiceReturning(canActAsPUntilI)

  "The intersection authorization of party P until instant I and unrestricted party P" should "be party P until instant I" in {
    chainAuthz(List(authzPUntilI, authzPUnrestricted)) should be(
      canActAsPUntilI
    )
  }

  "The union authorization of party P until instant I and unrestricted party P" should "be unrestricted party P" in {
    chainAuthz(List(authzPUntilI, authzPUnrestricted), ChainAuthzMode.Union) should be(
      canActAsPUnrestricted
    )
  }

  private val canActAsPUntilITick = canActAsPUnrestricted.copy(expiration = Some(Instant.MIN))
  private val authzPUntilITick = authServiceReturning(canActAsPUntilITick)

  "The intersection authorization of party P until instant I and party P until instant I'" should "be empty" in {
    chainAuthz(List(authzPUntilI, authzPUntilITick)) should be(
      Claims.empty
    )
  }

  "The union authorization of party P until instant I and party P until instant I'" should "be empty" in {
    chainAuthz(List(authzPUntilI, authzPUntilITick), ChainAuthzMode.Intersection) should be(
      Claims.empty
    )
  }
}
