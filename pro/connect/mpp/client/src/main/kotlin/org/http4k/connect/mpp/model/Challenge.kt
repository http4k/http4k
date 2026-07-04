/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.connect.mpp.model

import se.ansman.kotshi.JsonSerializable
import java.time.Instant

@JsonSerializable
data class Challenge(
    val id: ChallengeId,
    val realm: Realm,
    val method: PaymentMethod,
    val intent: PaymentIntent,
    val request: ChargeRequest? = null,
    val expires: Instant? = null,
    val description: String? = null,
    val opaque: String? = null
)

/**
 * True when this credential's challenge demands the same payment (realm/method/intent/request) as the
 * server-issued [expected] challenge. Deliberately ignores per-issuance fields (id/expires/opaque/description),
 * which the verifier binds instead.
 */
fun Challenge.bindsTo(expected: Challenge) =
    realm == expected.realm && method == expected.method &&
        intent == expected.intent && request == expected.request
