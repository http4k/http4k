/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.mpp

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.ai.mcp.protocol.SessionId
import org.http4k.ai.mcp.protocol.messages.McpTool
import org.http4k.ai.mcp.server.protocol.McpRequest
import org.http4k.ai.mcp.server.protocol.McpResponse.Ok
import org.http4k.ai.mcp.server.protocol.Session
import org.http4k.ai.mcp.server.protocol.then
import org.http4k.ai.mcp.util.McpJson
import org.http4k.ai.mcp.util.McpJson.asJsonObject
import org.http4k.ai.model.ToolName
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
import org.http4k.core.Request
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Uri
import org.http4k.filter.McpFilters
import org.http4k.format.Json
import org.http4k.format.renderError
import org.http4k.jsonrpc.ErrorMessage
import org.junit.jupiter.api.Test
import java.time.Instant

class MppMcpFilterTest {

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

    private val handler = McpFilters.MppPaymentRequired(verifier) { MppPaymentCheck.Required(listOf(challenge)) }
        .then { Ok(McpJson.nullNode()) }

    private fun mcpRequest(cred: Credential? = null): McpRequest {
        val meta = cred?.let {
            mapOf("org.paymentauth/credential" to McpJson.parse(MppMoshi.asFormatString(it)))
        } ?: emptyMap()

        val body = McpJson.asFormatString(
            asJsonObject(
                mapOf(
                    "jsonrpc" to "2.0",
                    "method" to "tools/call",
                    "id" to 1,
                    "params" to mapOf("name" to "test", "_meta" to meta)
                )
            )
        )

        return McpRequest(
            Session(SessionId.of("test-session")),
            McpTool.Call.Request(McpTool.Call.Request.Params(ToolName.of("test")), asJsonObject(1)),
            Request(POST, "/mcp").body(body)
        )
    }

    @Test
    fun `request without credential returns error with -32042 and challenges`() {
        val result = handler(mcpRequest())

        assertThat(result, equalTo(Ok(mppErrorResponse(-32042, "Payment required", listOf(challenge)))))
    }

    @Test
    fun `request with valid credential succeeds`() {
        val result = handler(mcpRequest(credential))

        assertThat(result, equalTo(Ok(McpJson.nullNode())))
    }

    @Test
    fun `verification failure returns error with -32043 and challenges`() {
        val failingVerifier = MppVerifier { Failure(RemoteFailure(POST, Uri.of("https://verify.example.com"), BAD_REQUEST, "bad signature")) }
        val failHandler = McpFilters.MppPaymentRequired(failingVerifier) { MppPaymentCheck.Required(listOf(challenge)) }
            .then { Ok(McpJson.nullNode()) }

        val result = failHandler(mcpRequest(credential))

        assertThat(result, equalTo(Ok(mppErrorResponse(-32043, "bad signature", listOf(challenge)))))
    }

    @Test
    fun `request passes through without payment when check returns Free`() {
        val freeHandler = McpFilters.MppPaymentRequired(verifier) { MppPaymentCheck.Free }
            .then { Ok(McpJson.nullNode()) }

        val result = freeHandler(mcpRequest())

        assertThat(result, equalTo(Ok(McpJson.nullNode())))
    }

    private fun mppErrorResponse(code: Int, message: String, challenges: List<Challenge>) =
        McpJson.renderError(
            object : ErrorMessage(code, message) {
                @Suppress("UNCHECKED_CAST")
                override fun <NODE> data(json: Json<NODE>): NODE =
                    MppMoshi.asJsonObject(mapOf("challenges" to challenges)) as NODE
            },
            asJsonObject(1)
        )
}
