/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.x402.X402Signer
import org.http4k.connect.x402.model.AssetAddress
import org.http4k.connect.x402.model.PaymentAmount
import org.http4k.connect.x402.model.PaymentNetwork
import org.http4k.connect.x402.model.PaymentPayload
import org.http4k.connect.x402.model.PaymentRequired
import org.http4k.connect.x402.model.PaymentRequirements
import org.http4k.connect.x402.model.PaymentScheme
import org.http4k.connect.x402.model.ResourceInfo
import org.http4k.connect.x402.model.WalletAddress
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.PAYMENT_REQUIRED
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.lens.paymentRequiredLens
import org.http4k.lens.paymentSignatureLens
import org.junit.jupiter.api.Test

class X402ClientFilterTest {

    private val requirements = PaymentRequirements(
        scheme = PaymentScheme.of("exact"),
        network = PaymentNetwork.of("base-sepolia"),
        asset = AssetAddress.of("0xUSDC"),
        amount = PaymentAmount.of("100"),
        payTo = WalletAddress.of("0xmerchant"),
        maxTimeoutSeconds = 30
    )

    private val signedPayload = PaymentPayload(
        x402Version = 2,
        scheme = PaymentScheme.of("exact"),
        network = PaymentNetwork.of("base-sepolia"),
        payload = mapOf("signature" to "0xsigned"),
        resource = "https://api.example.com/data",
        description = "Test resource"
    )

    private val fakeSigner = X402Signer { Success(signedPayload) }

    @Test
    fun `non-402 response passes through unchanged`() {
        val handler = ClientFilters.X402PaymentRequired(fakeSigner)
            .then { Response(OK).body("content") }

        val response = handler(Request(GET, "/"))

        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), equalTo("content"))
    }

    @Test
    fun `402 response triggers sign and retry`() {
        var callCount = 0
        val handler = ClientFilters.X402PaymentRequired(fakeSigner)
            .then { req ->
                callCount++
                if (callCount == 1) {
                    Response(PAYMENT_REQUIRED).with(
                        paymentRequiredLens of PaymentRequired(
                            x402Version = 2,
                            error = "Payment Required",
                            accepts = listOf(requirements),
                            resource = ResourceInfo(Uri.of("/"))
                        )
                    )
                } else {
                    assertThat(paymentSignatureLens(req)!!.scheme, equalTo(PaymentScheme.of("exact")))
                    Response(OK).body("paid content")
                }
            }

        val response = handler(Request(GET, "/"))

        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), equalTo("paid content"))
        assertThat(callCount, equalTo(2))
    }

    @Test
    fun `retry response returned as-is even if still 402`() {
        var callCount = 0
        val handler = ClientFilters.X402PaymentRequired(fakeSigner)
            .then { _ ->
                callCount++
                Response(PAYMENT_REQUIRED).with(
                    paymentRequiredLens of PaymentRequired(
                        x402Version = 2,
                        error = "Payment Required",
                        accepts = listOf(requirements),
                        resource = ResourceInfo(Uri.of("/"))
                    )
                )
            }

        val response = handler(Request(GET, Uri.of("/")))

        assertThat(response.status, equalTo(PAYMENT_REQUIRED))
        assertThat(callCount, equalTo(2))
    }

    @Test
    fun `signing failure returns original 402 response`() {
        val failingSigner = X402Signer { Failure("no wallet configured") }
        var callCount = 0
        val handler = ClientFilters.X402PaymentRequired(failingSigner)
            .then { _ ->
                callCount++
                Response(PAYMENT_REQUIRED).with(
                    paymentRequiredLens of PaymentRequired(
                        x402Version = 2,
                        error = "Payment Required",
                        accepts = listOf(requirements),
                        resource = ResourceInfo(Uri.of("/"))
                    )
                )
            }

        val response = handler(Request(GET, "/"))

        assertThat(response.status, equalTo(PAYMENT_REQUIRED))
        assertThat(callCount, equalTo(1))
    }
}
