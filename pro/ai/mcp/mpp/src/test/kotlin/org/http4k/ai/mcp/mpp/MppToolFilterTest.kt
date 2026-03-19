/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.mpp

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.ai.mcp.ToolRequest
import org.http4k.ai.mcp.ToolResponse.Error
import org.http4k.ai.mcp.ToolResponse.Ok
import org.http4k.ai.mcp.model.Content.Text
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.mpp.MppPaymentCheck.Free
import org.http4k.ai.mcp.mpp.MppPaymentCheck.Required
import org.http4k.ai.mcp.then
import org.http4k.connect.RemoteFailure
import org.http4k.connect.mpp.MppMoshi
import org.http4k.connect.mpp.MppVerifier
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
import org.http4k.core.Method.POST
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Uri
import org.http4k.lens.MetaKey
import org.junit.jupiter.api.Test
import java.time.Instant

class MppToolFilterTest {

    private val credentialLens = MetaKey.mppCredential().toLens()
    private val receiptLens = MetaKey.mppReceipt().toLens()

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

    private val verifier = MppVerifier { cred ->
        Success(
            Receipt(
                status = ReceiptStatus.success,
                method = cred.challenge.method,
                timestamp = Instant.parse("2025-01-15T12:05:00Z"),
                challengeId = cred.challenge.id
            )
        )
    }

    private val handler = MppToolFilter(verifier) { Required(listOf(challenge)) }
        .then { Ok("success") }

    @Test
    fun `request without credential in meta returns error with challenges`() {
        val result = handler(ToolRequest()) as Error

        assertThat(result.content, equalTo(listOf(Text("Payment required"))))
        val json = MppMoshi.asFormatString(result.structuredContent!!)
        assertThat(json.contains("\"challenge-123\""), equalTo(true))
        assertThat(json.contains("\"amount\""), equalTo(true))
    }

    @Test
    fun `request with valid credential succeeds and includes receipt in meta`() {
        val result = handler(ToolRequest(meta = Meta(credentialLens of credential))) as Ok

        assertThat(result.content, equalTo(listOf(Text("success"))))
        val receipt = receiptLens(result.meta)!!
        assertThat(receipt.status, equalTo(ReceiptStatus.success))
        assertThat(receipt.method, equalTo(PaymentMethod.of("tempo")))
    }

    @Test
    fun `verification failure returns error with challenges`() {
        val failingVerifier = MppVerifier { Failure(RemoteFailure(POST, Uri.of("https://verify.example.com"), BAD_REQUEST, "bad signature")) }
        val failHandler = MppToolFilter(failingVerifier) { Required(listOf(challenge)) }
            .then { Ok("success") }

        val result = failHandler(ToolRequest(meta = Meta(credentialLens of credential))) as Error

        assertThat(result.content, equalTo(listOf(Text("bad signature"))))
        val json = MppMoshi.asFormatString(result.structuredContent!!)
        assertThat(json.contains("\"challenges\""), equalTo(true))
    }

    @Test
    fun `request passes through without payment when check returns Free`() {
        val freeHandler = MppToolFilter(verifier) { Free }
            .then { Ok("free content") }

        val result = freeHandler(ToolRequest()) as Ok

        assertThat(result.content, equalTo(listOf(Text("free content"))))
    }
}
