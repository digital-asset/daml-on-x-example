// Copyright (c) 2020 The DAML Authors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.daml.ledger.damlonxexample.authz

import java.util.concurrent.{CompletableFuture, CompletionStage}

import com.daml.ledger.damlonxexample.authz.ChainAuthzMode.ChainzAuthMode
import com.digitalasset.ledger.api.auth.{AuthService, Claim, ClaimActAsAnyParty, ClaimAdmin, ClaimPublic, Claims}
import io.grpc.Metadata

object ChainAuthzMode extends Enumeration {
  type ChainzAuthMode = Value
  val Intersection, Union = Value
}

class ChainAuthService(
    private val authServices: Seq[AuthService],
    private val mode: ChainzAuthMode = ChainAuthzMode.Intersection
) extends AuthService {

  override def decodeMetadata(headers: Metadata): CompletionStage[Claims] =
    if (authServices.isEmpty) {
      CompletableFuture.completedStage(Claims.empty)
    } else {
      val authSvc1 = authServices.head
      authServices.tail.foldLeft(
        authSvc1.decodeMetadata(headers)
      ) { (claims: CompletionStage[Claims], authService: AuthService) =>
        claims.thenCombineAsync(
          authService.decodeMetadata(headers),
          (c1, c2: Claims) => combine(c1, c2).getOrElse(Claims.empty)
        )
      }
    }

  private def combine(claims1: Claims, claims2: Claims): Option[Claims] =
    if (ChainAuthzMode.Intersection.eq(mode))
      intersect(claims1, claims2)
    else
      union(claims1, claims2)

  private def intersect(claims1: Claims, claims2: Claims): Option[Claims] =
    for {
      ledgerId <- intersect(claims1.ledgerId, claims2.ledgerId)
      participantId <- intersect(claims1.participantId, claims2.participantId)
      expiration <- intersect(claims1.expiration, claims2.expiration)
    } yield
      Claims(
        intersect(claims1.claims, claims2.claims).toSet.toSeq,
        ledgerId,
        participantId,
        expiration
      )

  private def intersect[T](v1: Option[T], v2: Option[T]): Option[Option[T]] =
    (v1, v2) match {
      case (v1, v2) if v1 == v2 => Some(v1)
      case (None, v)            => Some(v)
      case (v, None)            => Some(v)
      case _                    => None
    }

  private def intersect(claims1: Seq[Claim], claims2: Seq[Claim]): List[Claim] =
    intersect(claims1, claims2, ClaimAdmin) ++
      intersect(claims1, claims2, ClaimPublic) ++
        (if (claims1.contains(ClaimActAsAnyParty))
           claims2
         else if (claims2.contains(ClaimActAsAnyParty))
           claims1
         else
           claims1.intersect(claims2))

  private def intersect(claims1: Seq[Claim], claims2: Seq[Claim], claim: Claim): List[Claim] =
    if (claims1.contains(claim) && claims2.contains(claim))
      List(claim)
    else
      List.empty

  private def union(claims1: Claims, claims2: Claims): Option[Claims] =
    for {
      ledgerId <- union(claims1.ledgerId, claims2.ledgerId)
      participantId <- union(claims1.participantId, claims2.participantId)
      expiration <- union(claims1.expiration, claims2.expiration)
    } yield
      Claims(union(claims1.claims, claims2.claims).toSet.toSeq, ledgerId, participantId, expiration)

  private def union[T](v1: Option[T], v2: Option[T]): Option[Option[T]] =
    (v1, v2) match {
      case (v1, v2) if v1 == v2  => Some(v1)
      case (_, None) | (None, _) => Some(None)
      case _                     => None
    }

  private def union(claims1: Seq[Claim], claims2: Seq[Claim]): List[Claim] =
    union(claims1, claims2, ClaimAdmin) ++
      union(claims1, claims2, ClaimPublic) ++
        (if (claims1.contains(ClaimActAsAnyParty) || claims2.contains(ClaimActAsAnyParty))
           List(ClaimActAsAnyParty)
         else
           claims1 ++ claims2)

  private def union(claims1: Seq[Claim], claims2: Seq[Claim], claim: Claim): List[Claim] =
    if (claims1.contains(claim) || claims2.contains(claim))
      List(claim)
    else
      List.empty

}
