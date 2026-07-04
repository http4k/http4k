/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.x402

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.protocol.SessionId
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcEmptyResponse
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcErrorResponse
import org.http4k.ai.mcp.protocol.messages.McpTool
import org.http4k.ai.mcp.server.protocol.McpRequest
import org.http4k.ai.mcp.server.protocol.McpResponse
import org.http4k.ai.mcp.server.protocol.Session
import org.http4k.ai.mcp.server.protocol.then
import org.http4k.ai.mcp.util.McpJson
import org.http4k.ai.mcp.util.McpJson.asJsonObject
import org.http4k.ai.model.ToolName
import org.http4k.connect.x402.FakeX402Facilitator
import org.http4k.connect.x402.X402Moshi
import org.http4k.connect.x402.model.AssetAddress
import org.http4k.connect.x402.model.PaymentAmount
import org.http4k.connect.x402.model.PaymentNetwork
import org.http4k.connect.x402.model.PaymentPayload
import org.http4k.connect.x402.model.PaymentRequirements
import org.http4k.connect.x402.model.PaymentScheme
import org.http4k.connect.x402.model.SupportedKind
import org.http4k.connect.x402.model.WalletAddress
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.filter.McpFilters
import org.http4k.format.MoshiObject
import org.http4k.jsonrpc.ErrorMessage
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

class X402McpFilterTest {

    private val requirements = PaymentRequirements(
        scheme = PaymentScheme.of("exact"),
        network = PaymentNetwork.of("base-sepolia"),
        asset = AssetAddress.of("0xUSDC"),
        amount = PaymentAmount.of("100"),
        payTo = WalletAddress.of("0xmerchant"),
        maxTimeoutSeconds = 30
    )

    private val fakeFacilitator = FakeX402Facilitator()

    private val signedPayload = PaymentPayload(
        x402Version = 2,
        scheme = PaymentScheme.of("exact"),
        network = PaymentNetwork.of("base-sepolia"),
        payload = mapOf("signature" to "0xsigned"),
        resource = "https://api.example.com/mcp",
        description = "MCP request"
    )

    private val handler = McpFilters.X402PaymentRequired(fakeFacilitator.client()) { PaymentCheck.Required(listOf(requirements)) }
        .then { McpResponse.Ok(McpJsonRpcEmptyResponse(it.message.id)) }

    private fun mcpRequest(payment: PaymentPayload? = null): McpRequest {
        val metaFields = payment?.let {
            MoshiObject("x402/payment" to McpJson.parse(X402Moshi.asFormatString(it)))
        } ?: MoshiObject()

        val message = McpTool.Call.Request(
            McpTool.Call.Request.Params(ToolName.of("test"), _meta = Meta(metaFields)),
            asJsonObject(1)
        )

        return McpRequest(
            Session(SessionId.of("test-session")),
            message,
            with(McpJson) { Request(POST, "/mcp").json(message) }
        )
    }

    @Test
    fun `request without payment in meta returns payment required error`() {
        val result = handler(mcpRequest())

        assertThat(result, equalTo(McpResponse.Ok(McpJsonRpcErrorResponse(asJsonObject(1), ErrorMessage(402, "Payment required")))))
    }

    @Test
    fun `request with valid payment succeeds`() {
        val result = handler(mcpRequest(signedPayload))

        assertThat(result, equalTo(McpResponse.Ok(McpJsonRpcEmptyResponse(asJsonObject(1)))))
    }

    @Test
    fun `with resourceFor a payment for a different resource is rejected`() {
        val handler = McpFilters.X402PaymentRequired(fakeFacilitator.client(), resourceFor = { "some-other-tool" }) {
            PaymentCheck.Required(listOf(requirements))
        }.then { McpResponse.Ok(McpJsonRpcEmptyResponse(it.message.id)) }

        val result = handler(mcpRequest(signedPayload))

        assertThat(result, equalTo(McpResponse.Ok(McpJsonRpcErrorResponse(asJsonObject(1), ErrorMessage(402, "Payment not valid for this resource")))))
    }

    @Test
    fun `with resourceFor a payment matching the resource succeeds`() {
        val handler = McpFilters.X402PaymentRequired(fakeFacilitator.client(), resourceFor = { signedPayload.resource }) {
            PaymentCheck.Required(listOf(requirements))
        }.then { McpResponse.Ok(McpJsonRpcEmptyResponse(it.message.id)) }

        val result = handler(mcpRequest(signedPayload))

        assertThat(result, equalTo(McpResponse.Ok(McpJsonRpcEmptyResponse(asJsonObject(1)))))
    }

    @Test
    fun `request with unsupported scheme or network returns error`() {
        val unmatchedPayload = PaymentPayload(
            x402Version = 2,
            scheme = PaymentScheme.of("unknown-scheme"),
            network = PaymentNetwork.of("unknown-network"),
            payload = mapOf("signature" to "0xabc"),
            resource = "https://api.example.com/mcp",
            description = "MCP request"
        )

        val result = handler(mcpRequest(unmatchedPayload))

        assertThat(result, equalTo(McpResponse.Ok(McpJsonRpcErrorResponse(asJsonObject(1), ErrorMessage(402, "Unsupported payment scheme/network")))))
    }

    @Test
    fun `matches correct requirement when multiple are offered`() {
        val altRequirements = PaymentRequirements(
            scheme = PaymentScheme.of("exact"),
            network = PaymentNetwork.of("solana-mainnet"),
            asset = AssetAddress.of("0xSOL"),
            amount = PaymentAmount.of("50"),
            payTo = WalletAddress.of("0xmerchant"),
            maxTimeoutSeconds = 30
        )

        val solPayload = PaymentPayload(
            x402Version = 2,
            scheme = PaymentScheme.of("exact"),
            network = PaymentNetwork.of("solana-mainnet"),
            payload = mapOf("signature" to "0xsol"),
            resource = "https://api.example.com/mcp",
            description = "MCP request"
        )

        val multiFacilitator = FakeX402Facilitator(
            listOf(
                SupportedKind(PaymentScheme.of("exact"), listOf(PaymentNetwork.of("base-sepolia"))),
                SupportedKind(PaymentScheme.of("exact"), listOf(PaymentNetwork.of("solana-mainnet")))
            )
        )
        val multiHandler = McpFilters.X402PaymentRequired(multiFacilitator.client()) {
            PaymentCheck.Required(listOf(requirements, altRequirements))
        }.then { McpResponse.Ok(McpJsonRpcEmptyResponse(it.message.id)) }

        val result = multiHandler(mcpRequest(solPayload))

        assertThat(result, equalTo(McpResponse.Ok(McpJsonRpcEmptyResponse(asJsonObject(1)))))
    }

    @Test
    fun `settlement failure suppresses tool content and returns error`() {
        val failing = FakeX402Facilitator().apply { settleFailureWhen = { true } }
        val handler = McpFilters.X402PaymentRequired(failing.client()) {
            PaymentCheck.Required(listOf(requirements))
        }.then { McpResponse.Ok(McpJsonRpcEmptyResponse(it.message.id)) }

        val result = handler(mcpRequest(signedPayload))

        assertThat(result, equalTo(McpResponse.Ok(McpJsonRpcErrorResponse(asJsonObject(1), ErrorMessage(402, "Settlement failed: Settlement failed")))))
    }

    @Test
    fun `default SettleBefore does not invoke tool when Settle fails`() {
        val invocations = AtomicInteger(0)
        val failing = FakeX402Facilitator().apply { settleFailureWhen = { true } }
        val handler = McpFilters.X402PaymentRequired(failing.client()) {
            PaymentCheck.Required(listOf(requirements))
        }.then {
            invocations.incrementAndGet()
            McpResponse.Ok(McpJsonRpcEmptyResponse(it.message.id))
        }

        handler(mcpRequest(signedPayload))

        assertThat(invocations.get(), equalTo(0))
    }

    @Test
    fun `SettleAfter mode runs tool before Settle and still suppresses content on Settle failure`() {
        val invocations = AtomicInteger(0)
        val failing = FakeX402Facilitator().apply { settleFailureWhen = { true } }
        val handler = McpFilters.X402PaymentRequired(failing.client(), SettlementMode.SettleAfter) {
            PaymentCheck.Required(listOf(requirements))
        }.then {
            invocations.incrementAndGet()
            McpResponse.Ok(McpJsonRpcEmptyResponse(it.message.id))
        }

        val result = handler(mcpRequest(signedPayload))

        assertThat(invocations.get(), equalTo(1))
        assertThat(
            result,
            equalTo(McpResponse.Ok(McpJsonRpcErrorResponse(asJsonObject(1), ErrorMessage(402, "Settlement failed: Settlement failed"))))
        )
    }

    @Test
    fun `request passes through without payment when check returns Free`() {
        val freeHandler = McpFilters.X402PaymentRequired(fakeFacilitator.client()) { PaymentCheck.Free }
            .then { McpResponse.Ok(McpJsonRpcEmptyResponse(it.message.id)) }

        val result = freeHandler(mcpRequest())

        assertThat(result, equalTo(McpResponse.Ok(McpJsonRpcEmptyResponse(asJsonObject(1)))))
    }
}
