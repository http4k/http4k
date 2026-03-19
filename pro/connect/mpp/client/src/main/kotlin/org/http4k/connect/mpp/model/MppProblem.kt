/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.connect.mpp.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class MppProblem(
    val type: String,
    val title: String,
    val status: Int
) {
    companion object {
        private const val BASE = "https://paymentauth.org/problems"

        val paymentRequired = MppProblem("$BASE/payment-required", "Payment Required", 402)
        val verificationFailed = MppProblem("$BASE/verification-failed", "Verification Failed", 402)
        val malformedCredential = MppProblem("$BASE/malformed-credential", "Malformed Credential", 402)
        val invalidChallenge = MppProblem("$BASE/invalid-challenge", "Invalid Challenge", 402)
    }
}
