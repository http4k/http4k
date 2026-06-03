/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.x402

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isA
import org.http4k.ai.mcp.ToolRequest
import org.http4k.ai.mcp.ToolResponse.Error
import org.http4k.ai.mcp.ToolResponse.Ok
import org.http4k.ai.mcp.model.Content.Text
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.then
import org.http4k.ai.mcp.x402.PaymentCheck.Free
import org.http4k.ai.mcp.x402.PaymentCheck.Required
import org.http4k.connect.x402.FakeX402Facilitator
import org.http4k.connect.x402.X402Moshi
import org.http4k.connect.x402.action.SettledResponse
import org.http4k.connect.x402.model.AssetAddress
import org.http4k.connect.x402.model.PaymentAmount
import org.http4k.connect.x402.model.PaymentNetwork
import org.http4k.connect.x402.model.PaymentPayload
import org.http4k.connect.x402.model.PaymentRequired
import org.http4k.connect.x402.model.PaymentRequirements
import org.http4k.connect.x402.model.PaymentScheme
import org.http4k.connect.x402.model.SupportedKind
import org.http4k.connect.x402.model.TransactionHash
import org.http4k.connect.x402.model.WalletAddress
import org.http4k.lens.MetaKey
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

class X402ToolFilterTest {

    private val paymentLens = MetaKey.x402PaymentPayload().toLens()
    private val settlementLens = MetaKey.x402Settled().toLens()

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
        resource = "https://api.example.com/tool",
        description = "Tool call"
    )

    private val echoHandler = X402ToolFilter(fakeFacilitator.client()) { Required(listOf(requirements)) }
        .then { Ok("success") }

    @Test
    fun `request without payment in meta returns PaymentRequired error`() {
        val result = echoHandler(ToolRequest()) as Error

        val paymentRequired = X402Moshi.convert<Any, PaymentRequired>(result.structuredContent!!)
        assertThat(paymentRequired, equalTo(PaymentRequired(2, "Payment required", listOf(requirements))))
        assertThat(result.content, equalTo(listOf(Text(X402Moshi.asFormatString(paymentRequired)))))
    }

    @Test
    fun `request with valid payment succeeds and includes settlement in meta`() {
        val result = echoHandler(ToolRequest(meta = Meta(paymentLens of signedPayload))) as Ok

        assertThat(result.content, equalTo(listOf(Text("success"))))
        assertThat(
            settlementLens(result.meta),
            equalTo(SettledResponse(TransactionHash.of("0xtx"), PaymentNetwork.of("base-sepolia"), WalletAddress.of("0xpayer")))
        )
    }

    @Test
    fun `returns PaymentRequired error when payload scheme does not match any requirement`() {
        val unmatchedPayload = PaymentPayload(
            x402Version = 2,
            scheme = PaymentScheme.of("unknown-scheme"),
            network = PaymentNetwork.of("unknown-network"),
            payload = mapOf("signature" to "0xabc"),
            resource = "https://api.example.com/tool",
            description = "Tool call"
        )

        val result = echoHandler(ToolRequest(meta = Meta(paymentLens of unmatchedPayload)))

        assertThat(result, isA<Error>())
        val paymentRequired = X402Moshi.convert<Any, PaymentRequired>((result as Error).structuredContent!!)
        assertThat(paymentRequired.error, equalTo("Unsupported payment scheme/network"))
        assertThat(paymentRequired.accepts, equalTo(listOf(requirements)))
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
            resource = "https://api.example.com/tool",
            description = "Tool call"
        )

        val multiFacilitator = FakeX402Facilitator(
            listOf(
                SupportedKind(PaymentScheme.of("exact"), listOf(PaymentNetwork.of("base-sepolia"))),
                SupportedKind(PaymentScheme.of("exact"), listOf(PaymentNetwork.of("solana-mainnet")))
            )
        )
        val multiHandler = X402ToolFilter(multiFacilitator.client()) { Required(listOf(requirements, altRequirements)) }
            .then { Ok("success") }

        val result = multiHandler(ToolRequest(meta = Meta(paymentLens of solPayload)))

        assertThat(result, isA<Ok>())
    }

    @Test
    fun `settlement failure suppresses tool content and returns payment error`() {
        val failing = FakeX402Facilitator().apply { settleFailureWhen = { true } }
        val handler = X402ToolFilter(failing.client()) { Required(listOf(requirements)) }
            .then { Ok("secret content") }

        val result = handler(ToolRequest(meta = Meta(paymentLens of signedPayload))) as Error

        val paymentRequired = X402Moshi.convert<Any, PaymentRequired>(result.structuredContent!!)
        assertThat(paymentRequired.error, equalTo("Settlement failed"))
    }

    @Test
    fun `default SettleBefore does not invoke tool when Settle fails`() {
        val invocations = AtomicInteger(0)
        val failing = FakeX402Facilitator().apply { settleFailureWhen = { true } }
        val handler = X402ToolFilter(failing.client()) { Required(listOf(requirements)) }
            .then {
                invocations.incrementAndGet()
                Ok("secret content")
            }

        val result = handler(ToolRequest(meta = Meta(paymentLens of signedPayload))) as Error

        assertThat(invocations.get(), equalTo(0))
        val paymentRequired = X402Moshi.convert<Any, PaymentRequired>(result.structuredContent!!)
        assertThat(paymentRequired.error, equalTo("Settlement failed"))
    }

    @Test
    fun `SettleAfter mode runs tool before Settle and still suppresses content on Settle failure`() {
        val invocations = AtomicInteger(0)
        val failing = FakeX402Facilitator().apply { settleFailureWhen = { true } }
        val handler = X402ToolFilter(failing.client(), SettlementMode.SettleAfter) {
            Required(listOf(requirements))
        }.then {
            invocations.incrementAndGet()
            Ok("secret content")
        }

        val result = handler(ToolRequest(meta = Meta(paymentLens of signedPayload))) as Error

        assertThat(invocations.get(), equalTo(1))
        val paymentRequired = X402Moshi.convert<Any, PaymentRequired>(result.structuredContent!!)
        assertThat(paymentRequired.error, equalTo("Settlement failed"))
    }

    @Test
    fun `request passes through without payment when check returns Free`() {
        val freeHandler = X402ToolFilter(fakeFacilitator.client()) { Free }
            .then { Ok("free content") }

        val result = freeHandler(ToolRequest()) as Ok

        assertThat(result.content, equalTo(listOf(Text("free content"))))
    }
}
