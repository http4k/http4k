/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.x402

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.mcp.protocol.SessionId
import org.http4k.ai.mcp.protocol.messages.toJsonRpc
import org.http4k.ai.mcp.server.protocol.McpRequest
import org.http4k.ai.mcp.server.protocol.McpResponse
import org.http4k.ai.mcp.server.protocol.Session
import org.http4k.ai.mcp.server.protocol.then
import org.http4k.ai.mcp.util.McpJson
import org.http4k.ai.mcp.util.McpJson.asJsonObject
import org.http4k.connect.x402.FakeX402Facilitator
import org.http4k.connect.x402.Http
import org.http4k.connect.x402.X402Facilitator
import org.http4k.connect.x402.X402Moshi
import org.http4k.connect.x402.X402Moshi.json
import org.http4k.connect.x402.model.AssetAddress
import org.http4k.connect.x402.model.PaymentAmount
import org.http4k.connect.x402.model.PaymentNetwork
import org.http4k.connect.x402.model.PaymentPayload
import org.http4k.connect.x402.model.PaymentRequirements
import org.http4k.connect.x402.model.PaymentScheme
import org.http4k.connect.x402.model.SettleResponse
import org.http4k.connect.x402.model.SupportedKind
import org.http4k.connect.x402.model.VerifyResponse
import org.http4k.connect.x402.model.WalletAddress
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.filter.McpFilters
import org.http4k.format.MoshiNode
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.jsonrpc.JsonRpcRequest
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.junit.jupiter.api.Test

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
        .then { McpResponse(McpJson.nullNode()) }

    private fun mcpRequest(payment: PaymentPayload? = null) = McpRequest(
        Session(SessionId.of("test-session")),
        jsonRpcRequest(payment),
        Request(POST, "/mcp")
    )

    private fun jsonRpcRequest(payment: PaymentPayload? = null): JsonRpcRequest<MoshiNode> {
        val meta = payment?.let {
            mapOf("x402/payment" to McpJson.parse(X402Moshi.asFormatString(it)))
        } ?: emptyMap()

        return JsonRpcRequest(
            McpJson, mapOf(
                "jsonrpc" to asJsonObject("2.0"),
                "method" to asJsonObject("tools/call"),
                "id" to asJsonObject(1),
                "params" to asJsonObject(
                    mapOf("_meta" to asJsonObject(meta))
                )
            )
        )
    }

    @Test
    fun `request without payment in meta returns payment required error`() {
        val result = handler(mcpRequest())

        assertThat(result, equalTo(McpResponse(ErrorMessage(402, "Payment required").toJsonRpc(asJsonObject(1)))))
    }

    @Test
    fun `request with valid payment succeeds`() {
        val result = handler(mcpRequest(signedPayload))

        assertThat(result, equalTo(McpResponse(McpJson.nullNode())))
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

        assertThat(result, equalTo(McpResponse(ErrorMessage(402, "Unsupported payment scheme/network").toJsonRpc(asJsonObject(1)))))
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
        }.then { McpResponse(McpJson.nullNode()) }

        val result = multiHandler(mcpRequest(solPayload))

        assertThat(result, equalTo(McpResponse(McpJson.nullNode())))
    }

    @Test
    fun `settlement failure suppresses tool content and returns error`() {
        val verifyPassSettleFail = routes(
            "/verify" bind POST to {
                Response(OK).json(VerifyResponse(isValid = true, payer = WalletAddress.of("0xpayer")))
            },
            "/settle" bind POST to {
                Response(OK).json(SettleResponse(success = false, errorReason = "Settlement rejected"))
            }
        )
        val handler = McpFilters.X402PaymentRequired(X402Facilitator.Http(Uri.of(""), verifyPassSettleFail)) {
            PaymentCheck.Required(listOf(requirements))
        }.then { McpResponse(McpJson.nullNode()) }

        val result = handler(mcpRequest(signedPayload))

        assertThat(result, equalTo(McpResponse(ErrorMessage(402, "Settlement failed: Settlement rejected").toJsonRpc(asJsonObject(1)))))
    }

    @Test
    fun `request passes through without payment when check returns Free`() {
        val freeHandler = McpFilters.X402PaymentRequired(fakeFacilitator.client()) { PaymentCheck.Free }
            .then { McpResponse(McpJson.nullNode()) }

        val result = freeHandler(mcpRequest())

        assertThat(result, equalTo(McpResponse(McpJson.nullNode())))
    }
}
