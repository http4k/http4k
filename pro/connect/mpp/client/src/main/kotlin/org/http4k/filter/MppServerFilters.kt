/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.filter

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.recover
import org.http4k.connect.mpp.MppMoshi
import org.http4k.connect.mpp.MppVerifier
import org.http4k.connect.mpp.model.Challenge
import org.http4k.connect.mpp.model.MppProblem
import org.http4k.core.Filter
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.PAYMENT_REQUIRED
import org.http4k.core.with
import org.http4k.lens.LensFailure
import org.http4k.lens.mppChallengeLens
import org.http4k.lens.mppCredentialLens
import org.http4k.lens.mppReceiptLens

fun ServerFilters.MppPaymentRequired(
    verifier: MppVerifier,
    challengeFor: (Request) -> Challenge
) = Filter { next ->
    { req ->
        val expected = challengeFor(req)
        try {
            mppCredentialLens(req)?.let { credential ->
                when (credential.challenge.id) {
                    expected.id -> verifier.verify(expected, credential)
                        .map { receipt -> next(req).with(mppReceiptLens of receipt) }
                        .recover { paymentRequiredResponse(expected, MppProblem.verificationFailed) }

                    else -> paymentRequiredResponse(expected, MppProblem.invalidChallenge)
                }
            } ?: paymentRequiredResponse(expected, MppProblem.paymentRequired)
        } catch (e: LensFailure) {
            paymentRequiredResponse(expected, MppProblem.malformedCredential)
        }
    }
}

private fun paymentRequiredResponse(challenge: Challenge, problem: MppProblem) =
    Response(PAYMENT_REQUIRED)
        .with(mppChallengeLens of challenge)
        .header("Cache-Control", "no-store")
        .header("Content-Type", "application/problem+json")
        .body(MppMoshi.asFormatString(problem))
