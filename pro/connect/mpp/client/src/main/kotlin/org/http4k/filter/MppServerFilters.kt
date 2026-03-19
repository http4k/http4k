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
import org.http4k.lens.mppChallengeLens
import org.http4k.lens.mppCredentialLens
import org.http4k.lens.mppReceiptLens

fun ServerFilters.MppPaymentRequired(
    verifier: MppVerifier,
    challengeFor: (Request) -> Challenge
) = Filter { next ->
    { req ->
        mppCredentialLens(req)?.let { credential ->
            verifier.verify(credential)
                .map { receipt -> next(req).with(mppReceiptLens of receipt) }
                .recover { paymentRequiredResponse(challengeFor(req), MppProblem.verificationFailed) }
        } ?: paymentRequiredResponse(challengeFor(req), MppProblem.paymentRequired)
    }
}

private fun paymentRequiredResponse(challenge: Challenge, problem: MppProblem) =
    Response(PAYMENT_REQUIRED)
        .with(mppChallengeLens of challenge)
        .header("Cache-Control", "no-store")
        .header("Content-Type", "application/problem+json")
        .body(MppMoshi.asFormatString(problem))
