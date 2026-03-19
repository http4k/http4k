/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.mpp

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.mcp.model.Meta
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
import org.http4k.lens.MetaKey
import org.junit.jupiter.api.Test
import java.time.Instant

class MppMetaTest {

    private val credentialLens = MetaKey.mppCredential().toLens()
    private val receiptLens = MetaKey.mppReceipt().toLens()

    private val challenge = Challenge(
        id = ChallengeId.of("challenge-123"),
        realm = Realm.of("api.example.com"),
        method = PaymentMethod.of("tempo"),
        intent = PaymentIntent.of("charge"),
        request = ChargeRequest(amount = PaymentAmount.of("1000"), currency = Currency.of("USD"))
    )

    @Test
    fun `credential roundtrip via meta`() {
        val credential = Credential(
            challenge = challenge,
            payload = mapOf("proof" to "0xsigned")
        )

        val meta = Meta(credentialLens of credential)
        assertThat(credentialLens(meta), equalTo(credential))
    }

    @Test
    fun `credential returns null when missing`() {
        assertThat(credentialLens(Meta()), absent())
    }

    @Test
    fun `receipt roundtrip via meta`() {
        val receipt = Receipt(
            status = ReceiptStatus.success,
            method = PaymentMethod.of("tempo"),
            timestamp = Instant.parse("2025-01-15T12:05:00Z"),
            challengeId = ChallengeId.of("challenge-123")
        )

        val meta = Meta(receiptLens of receipt)
        assertThat(receiptLens(meta), equalTo(receipt))
    }

    @Test
    fun `receipt returns null when missing`() {
        assertThat(receiptLens(Meta()), absent())
    }
}
