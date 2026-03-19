/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.mpp.MppSigner
import org.http4k.connect.mpp.model.Challenge
import org.http4k.connect.mpp.model.ChallengeId
import org.http4k.connect.mpp.model.ChargeRequest
import org.http4k.connect.mpp.model.Credential
import org.http4k.connect.mpp.model.Currency
import org.http4k.connect.mpp.model.PaymentAmount
import org.http4k.connect.mpp.model.PaymentIntent
import org.http4k.connect.mpp.model.PaymentMethod
import org.http4k.connect.mpp.model.Realm
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.PAYMENT_REQUIRED
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.lens.mppChallengeLens
import org.http4k.lens.mppCredentialLens
import org.junit.jupiter.api.Test

class MppClientFilterTest {

    private val challenge = Challenge(
        id = ChallengeId.of("challenge-123"),
        realm = Realm.of("api.example.com"),
        method = PaymentMethod.of("tempo"),
        intent = PaymentIntent.of("charge"),
        request = ChargeRequest(amount = PaymentAmount.of("1000"), currency = Currency.of("USD"))
    )

    private val credential = Credential(
        challenge = challenge,
        payload = mapOf("proof" to "0xsigned")
    )

    private val fakeSigner = MppSigner { Success(credential) }

    @Test
    fun `non-402 response passes through unchanged`() {
        val handler = ClientFilters.MppPaymentRequired(fakeSigner)
            .then { Response(OK).body("content") }

        val response = handler(Request(GET, "/"))

        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), equalTo("content"))
    }

    @Test
    fun `402 response triggers sign and retry`() {
        var callCount = 0
        val handler = ClientFilters.MppPaymentRequired(fakeSigner)
            .then { req ->
                callCount++
                if (callCount == 1) {
                    Response(PAYMENT_REQUIRED).with(mppChallengeLens of challenge)
                } else {
                    assertThat(mppCredentialLens(req)!!.challenge.id, equalTo(ChallengeId.of("challenge-123")))
                    Response(OK).body("paid content")
                }
            }

        val response = handler(Request(GET, "/"))

        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), equalTo("paid content"))
        assertThat(callCount, equalTo(2))
    }

    @Test
    fun `signing failure returns original 402 response`() {
        val failingSigner = MppSigner { Failure("no wallet configured") }
        var callCount = 0
        val handler = ClientFilters.MppPaymentRequired(failingSigner)
            .then { _ ->
                callCount++
                Response(PAYMENT_REQUIRED).with(mppChallengeLens of challenge)
            }

        val response = handler(Request(GET, "/"))

        assertThat(response.status, equalTo(PAYMENT_REQUIRED))
        assertThat(callCount, equalTo(1))
    }

    @Test
    fun `retry response returned as-is even if still 402`() {
        var callCount = 0
        val handler = ClientFilters.MppPaymentRequired(fakeSigner)
            .then { _ ->
                callCount++
                Response(PAYMENT_REQUIRED).with(mppChallengeLens of challenge)
            }

        val response = handler(Request(GET, "/"))

        assertThat(response.status, equalTo(PAYMENT_REQUIRED))
        assertThat(callCount, equalTo(2))
    }
}
