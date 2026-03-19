/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.connect.mpp

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Success
import org.http4k.connect.mpp.model.Challenge
import org.http4k.connect.mpp.model.ChallengeId
import org.http4k.connect.mpp.model.ChargeRequest
import org.http4k.connect.mpp.model.Credential
import org.http4k.connect.mpp.model.Currency
import org.http4k.connect.mpp.model.PaymentAmount
import org.http4k.connect.mpp.model.PaymentIntent
import org.http4k.connect.mpp.model.PaymentMethod
import org.http4k.connect.mpp.model.Realm
import org.http4k.connect.mpp.model.Receipt
import org.http4k.connect.mpp.model.ReceiptStatus
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.MppPaymentRequired
import org.http4k.filter.ServerFilters
import org.http4k.lens.mppReceiptLens
import org.http4k.security.MppSecurity
import org.junit.jupiter.api.Test
import java.time.Instant

class MppEndToEndTest {

    private val challenge = Challenge(
        id = ChallengeId.of("challenge-123"),
        realm = Realm.of("api.example.com"),
        method = PaymentMethod.of("tempo"),
        intent = PaymentIntent.of("charge"),
        request = ChargeRequest(amount = PaymentAmount.of("1000"), currency = Currency.of("USD"))
    )

    private val receipt = Receipt(
        status = ReceiptStatus.success,
        method = PaymentMethod.of("tempo"),
        timestamp = Instant.parse("2025-01-15T12:05:00Z"),
        challengeId = ChallengeId.of("challenge-123")
    )

    private val fakeSigner = MppSigner { ch ->
        Success(
            Credential(
                challenge = ch,
                payload = mapOf("proof" to "0xsigned")
            )
        )
    }

    private val fakeVerifier = MppVerifier { Success(receipt) }

    @Test
    fun `full round trip - 402 then sign then retry then verify then 200 with receipt`() {
        val server = ServerFilters.MppPaymentRequired(fakeVerifier) { challenge }
            .then { Response(OK).body("premium content") }

        val client = ClientFilters.MppPaymentRequired(fakeSigner).then(server)

        val response = client(Request(GET, "/data"))

        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), equalTo("premium content"))
        val returnedReceipt = mppReceiptLens(response)
        assertThat(returnedReceipt.status, equalTo(ReceiptStatus.success))
        assertThat(returnedReceipt.challengeId, equalTo(ChallengeId.of("challenge-123")))
    }

    @Test
    fun `full round trip with security wrapper`() {
        val security = MppSecurity({ challenge }, fakeVerifier)

        val server = security.filter.then { Response(OK).body("secured content") }

        val client = ClientFilters.MppPaymentRequired(fakeSigner).then(server)

        val response = client(Request(GET, "/data"))

        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), equalTo("secured content"))
    }
}
