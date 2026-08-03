/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.x402

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.mcp.model.Meta
import org.http4k.connect.x402.action.SettledResponse
import org.http4k.connect.x402.model.PaymentNetwork
import org.http4k.connect.x402.model.PaymentPayload
import org.http4k.connect.x402.model.PaymentScheme
import org.http4k.connect.x402.model.TransactionHash
import org.http4k.connect.x402.model.WalletAddress
import org.http4k.lens.MetaKey
import org.junit.jupiter.api.Test

class X402MetaTest {

    private val x402PaymentLens = MetaKey.x402PaymentPayload().toLens()
    private val responseLens = MetaKey.x402Settled().toLens()

    @Test
    fun `payment roundtrip via meta`() {
        val payment = PaymentPayload(
            x402Version = 2,
            scheme = PaymentScheme.of("exact"),
            network = PaymentNetwork.of("base-sepolia"),
            payload = mapOf("signature" to "0xsigned"),
            resource = "https://api.example.com/tool",
            description = "Tool call"
        )

        val meta = Meta(x402PaymentLens of payment)
        assertThat(x402PaymentLens(meta), equalTo(payment))
    }

    @Test
    fun `payment returns null when missing`() {
        assertThat(x402PaymentLens(Meta()), absent())
    }

    @Test
    fun `settlement response roundtrip via meta`() {
        val settled = SettledResponse(
            transaction = TransactionHash.of("0xtx123"),
            network = PaymentNetwork.of("base-sepolia"),
            payer = WalletAddress.of("0xpayer")
        )

        val meta = Meta(responseLens of settled)
        assertThat(responseLens(meta), equalTo(settled))
    }

    @Test
    fun `settlement response returns null when missing`() {
        assertThat(responseLens(Meta()), absent())
    }
}
