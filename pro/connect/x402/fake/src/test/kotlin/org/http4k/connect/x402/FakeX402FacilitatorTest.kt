/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.connect.x402

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.RemoteFailure
import org.http4k.connect.x402.action.Settle
import org.http4k.connect.x402.action.VerifiedResponse
import org.http4k.connect.x402.action.Verify
import org.http4k.connect.x402.model.AssetAddress
import org.http4k.connect.x402.model.PaymentAmount
import org.http4k.connect.x402.model.PaymentNetwork
import org.http4k.connect.x402.model.PaymentPayload
import org.http4k.connect.x402.model.PaymentRequirements
import org.http4k.connect.x402.model.PaymentScheme
import org.http4k.connect.x402.model.WalletAddress
import org.http4k.core.Uri
import org.junit.jupiter.api.Test

class FakeX402FacilitatorTest : X402FacilitatorContract {
    override val http = FakeX402Facilitator()

    @Test
    fun `settleFailureWhen forces Settle to fail for matching requests`() {
        val fake = FakeX402Facilitator().apply { settleFailureWhen = { it.payload == payload } }
        val facilitator = X402Facilitator.Http(Uri.of(""), fake)

        val verify = facilitator(Verify(payload, requirements))
        val settle = facilitator(Settle(payload, requirements))

        assertThat(verify, equalTo(Success(VerifiedResponse(WalletAddress.of("0xpayer")))))
        assertThat(settle is Failure, equalTo(true))
        assertThat(((settle as Failure<RemoteFailure>).reason).message, equalTo("Settlement failed"))
    }
}

private val payload = PaymentPayload(
    x402Version = 2,
    scheme = PaymentScheme.of("exact"),
    network = PaymentNetwork.of("base-sepolia"),
    payload = mapOf("signature" to "0xabc"),
    resource = "https://api.example.com/data",
    description = "Test resource"
)

private val requirements = PaymentRequirements(
    scheme = PaymentScheme.of("exact"),
    network = PaymentNetwork.of("base-sepolia"),
    asset = AssetAddress.of("0xUSDC"),
    amount = PaymentAmount.of("100"),
    payTo = WalletAddress.of("0xmerchant"),
    maxTimeoutSeconds = 30
)
