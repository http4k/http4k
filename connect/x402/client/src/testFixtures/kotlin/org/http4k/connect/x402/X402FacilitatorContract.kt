package org.http4k.connect.x402

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Success
import org.http4k.connect.x402.action.Settle
import org.http4k.connect.x402.action.Settled
import org.http4k.connect.x402.action.Supported
import org.http4k.connect.x402.action.Verified
import org.http4k.connect.x402.action.Verify
import org.http4k.connect.x402.model.AssetAddress
import org.http4k.connect.x402.model.PaymentAmount
import org.http4k.connect.x402.model.PaymentNetwork
import org.http4k.connect.x402.model.PaymentPayload
import org.http4k.connect.x402.model.PaymentRequirements
import org.http4k.connect.x402.model.PaymentScheme
import org.http4k.connect.x402.model.SupportedKind
import org.http4k.connect.x402.model.SupportedResponse
import org.http4k.connect.x402.model.TransactionHash
import org.http4k.connect.x402.model.WalletAddress
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.junit.jupiter.api.Test

interface X402FacilitatorContract {
    val http: HttpHandler

    private val facilitator get() = X402Facilitator.Http(Uri.of(""), http)


    @Test
    fun `can verify a payment`() {
        assertThat(
            facilitator(Verify(payload, requirements)),
            equalTo(Success(Verified(WalletAddress.of("0xpayer"))))
        )
    }

    @Test
    fun `can settle a payment`() {
        assertThat(
            facilitator(Settle(payload, requirements)),
            equalTo(
                Success(
                    Settled(
                        TransactionHash.of("0xtx"),
                        PaymentNetwork.of("base-sepolia"),
                        WalletAddress.of("0xpayer")
                    )
                )
            )
        )
    }

    @Test
    fun `can get supported schemes`() {
        assertThat(
            facilitator(Supported),
            equalTo(
                Success(
                    SupportedResponse(
                        x402Version = 2,
                        kinds = listOf(
                            SupportedKind(
                                PaymentScheme.of("exact"),
                                listOf(PaymentNetwork.of("base-sepolia"))
                            )
                        )
                    )
                )
            )
        )
    }
}

private val payload
    get() = PaymentPayload(
        x402Version = 2,
        scheme = PaymentScheme.of("exact"),
        network = PaymentNetwork.of("base-sepolia"),
        payload = mapOf("signature" to "0xabc"),
        resource = "https://api.example.com/data",
        description = "Test resource"
    )

private val requirements
    get() = PaymentRequirements(
        scheme = PaymentScheme.of("exact"),
        network = PaymentNetwork.of("base-sepolia"),
        asset = AssetAddress.of("0xUSDC"),
        amount = PaymentAmount.of("100"),
        payTo = WalletAddress.of("0xmerchant"),
        maxTimeoutSeconds = 30
    )
