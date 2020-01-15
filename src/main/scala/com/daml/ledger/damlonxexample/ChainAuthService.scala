// Copyright (c) 2020 The DAML Authors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.daml.ledger.damlonxexample

import java.util.concurrent.{CompletableFuture, CompletionStage}

import com.digitalasset.ledger.api.auth.{AuthService, Claims}
import io.grpc.Metadata

class ChainAuthService(private val authServices: Seq[AuthService]) extends AuthService {

  override def decodeMetadata(headers: Metadata): CompletionStage[Claims] =
    authServices.foldLeft(
      CompletableFuture.completedStage(Claims.empty)
    ) { (claims: CompletionStage[Claims], authService: AuthService) =>
      claims.thenCombineAsync(
        authService.decodeMetadata(headers),
        (c1, c2: Claims) => combine(c1, c2).getOrElse(Claims.empty)
      )
    }

  private def combine(claims1: Claims, claims2: Claims): Option[Claims] =
    for {
      ledgerId <- intersect(claims1.ledgerId, claims2.ledgerId)
      participantId <- intersect(claims1.participantId, claims2.participantId)
      expiration <- intersect(claims1.expiration, claims2.expiration)
    } yield Claims(claims1.claims.intersect(claims2.claims), ledgerId, participantId, expiration)

  private def intersect[T](v1: Option[T], v2: Option[T]): Option[Option[T]] =
    (v1, v2) match {
      case (v1, v2) if v1 == v2 => Some(v1)
      case (None, v)            => Some(v)
      case _                    => None
    }
}
