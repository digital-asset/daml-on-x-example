// Copyright (c) 2020 The DAML Authors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.daml.ledger.damlonxexample.authz.javasdk;

import io.grpc.Metadata;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class WildcardJavaAuthService implements AuthService {
    @Override
    public final CompletionStage<Claims> decodeMetadata(final Metadata headers) {
        return CompletableFuture.completedStage(Claims.wildcard());
    }
}
