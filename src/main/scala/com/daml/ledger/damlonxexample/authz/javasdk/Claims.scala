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

sealed abstract class Claim {
  implicit def asScala: ScalaClaim
}

object ClaimAdmin extends Claim {
  def instance(): ClaimAdmin.type = ClaimAdmin

  implicit override def asScala: ScalaClaimAdmin.type = ScalaClaimAdmin
}

case object ClaimPublic extends Claim {
  def instance(): ClaimPublic.type = ClaimPublic

  implicit override def asScala: ScalaClaimPublic.type = ScalaClaimPublic
}

case object ClaimActAsAnyParty extends Claim {
  def instance(): ClaimActAsAnyParty.type = ClaimActAsAnyParty

  implicit override def asScala: ScalaClaimActAsAnyParty.type = ScalaClaimActAsAnyParty
}

final case class ClaimActAsParty(name: Ref.Party) extends Claim {
  implicit override def asScala: ScalaClaimActAsParty = ScalaClaimActAsParty(name)
}

final case class ClaimReadAsParty(name: Ref.Party) extends Claim {
  implicit override def asScala: ScalaClaimReadAsParty = ScalaClaimReadAsParty(name)
}

final case class Claims(
    claims: java.util.List[Claim],
    ledgerId: String,
    participantId: String,
    expiration: Instant
) {
  implicit def asScala: ScalaClaims =
    ScalaClaims(
      claims.asScala.map(_.asScala),
      Option(ledgerId),
      Option(participantId),
      Option(expiration)
    )
}

object Claims {

  val empty = Claims(Collections.emptyList(), null, null, null)

  val wildcard = Claims(
    List[Claim](ClaimPublic, ClaimAdmin, ClaimActAsAnyParty).asJava,
    ledgerId = null,
    participantId = null,
    expiration = null
  )
}
