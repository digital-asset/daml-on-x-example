// Copyright (c) 2020 The DAML Authors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.daml.ledger.damlonxexample.authz.javasdk;

import com.digitalasset.daml.lf.data.Ref;
import io.grpc.Metadata;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class NonTrivialJavaAuthService implements AuthService {
    @Override
    public final CompletionStage<Claims> decodeMetadata(final Metadata headers) {
        final var claims = new ArrayList<Claim>();
        claims.add(ClaimAdmin.instance());
        claims.add(ClaimPublic.instance());
        claims.add(ClaimActAsAnyParty.instance());
        claims.add(new ClaimReadAsParty(Ref.Name().assertFromString("P")));
        claims.add(new ClaimActAsParty(Ref.Name().assertFromString("P")));
        return CompletableFuture.completedStage(new Claims(claims, null, null, null));
    }
}
