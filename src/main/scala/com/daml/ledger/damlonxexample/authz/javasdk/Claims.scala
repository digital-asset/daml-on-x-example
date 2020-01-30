// Copyright (c) 2020 The DAML Authors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

/** Mirrors [[com.digitalasset.ledger.api.auth.Claims]] */
package com.daml.ledger.damlonxexample.authz.javasdk

import java.time.Instant
import java.util.Collections

import com.digitalasset.daml.lf.data.Ref
import com.digitalasset.ledger.api.auth.{
  Claim => ScalaClaim,
  ClaimActAsAnyParty => ScalaClaimActAsAnyParty,
  ClaimActAsParty => ScalaClaimActAsParty,
  ClaimAdmin => ScalaClaimAdmin,
  ClaimPublic => ScalaClaimPublic,
  ClaimReadAsParty => ScalaClaimReadAsParty,
  Claims => ScalaClaims
}

import scala.collection.JavaConverters._

sealed abstract class Claim

object ClaimAdmin extends Claim {
  def instance(): ClaimAdmin.type = ClaimAdmin
}

case object ClaimPublic extends Claim {
  def instance(): ClaimPublic.type = ClaimPublic
}

case object ClaimActAsAnyParty extends Claim {
  def instance(): ClaimActAsAnyParty.type = ClaimActAsAnyParty
}

final case class ClaimActAsParty(name: Ref.Party) extends Claim

final case class ClaimReadAsParty(name: Ref.Party) extends Claim

final case class Claims(
    claims: java.util.List[Claim],
    ledgerId: String,
    participantId: String,
    expiration: Instant
)

object Claims {

  val empty = Claims(Collections.emptyList(), null, null, null)

  val wildcard = Claims(
    List[Claim](ClaimPublic, ClaimAdmin, ClaimActAsAnyParty).asJava,
    ledgerId = null,
    participantId = null,
    expiration = null
  )

  object ToScala {

    implicit def javaClaimToScalaClaim(claim: Claim): ScalaClaim =
      claim match {
        case ClaimAdmin => ScalaClaimAdmin
        case ClaimPublic => ScalaClaimPublic
        case ClaimActAsAnyParty => ScalaClaimActAsAnyParty
        case ClaimActAsParty(name) => ScalaClaimActAsParty(name)
        case ClaimReadAsParty(name) => ScalaClaimReadAsParty(name)
      }

    implicit def javaClaimsToScalaClaims(claims: Claims): ScalaClaims =
      ScalaClaims(
        claims.claims.asScala.map(javaClaimToScalaClaim),
        Option(claims.ledgerId),
        Option(claims.participantId),
        Option(claims.expiration)
      )
  }
}
