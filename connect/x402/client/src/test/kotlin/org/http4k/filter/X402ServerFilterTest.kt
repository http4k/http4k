package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.http4k.connect.RemoteFailure
import org.http4k.connect.x402.X402Facilitator
import org.http4k.connect.x402.X402FacilitatorAction
import org.http4k.connect.x402.action.Settle
import org.http4k.connect.x402.action.Settled
import org.http4k.connect.x402.action.Verified
import org.http4k.connect.x402.action.Verify
import org.http4k.connect.x402.model.AssetAddress
import org.http4k.connect.x402.model.PaymentAmount
import org.http4k.connect.x402.model.PaymentNetwork
import org.http4k.connect.x402.model.PaymentPayload
import org.http4k.connect.x402.model.PaymentRequirements
import org.http4k.connect.x402.model.PaymentScheme
import org.http4k.connect.x402.model.TransactionHash
import org.http4k.connect.x402.model.WalletAddress
import org.http4k.core.Method
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.PAYMENT_REQUIRED
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.lens.paymentRequiredLens
import org.http4k.lens.paymentResponseLens
import org.http4k.lens.paymentSignatureLens
import org.junit.jupiter.api.Test

class X402ServerFilterTest {

    private val requirements = PaymentRequirements(
        scheme = PaymentScheme.of("exact"),
        network = PaymentNetwork.of("base-sepolia"),
        asset = AssetAddress.of("0xUSDC"),
        amount = PaymentAmount.of("100"),
        payTo = WalletAddress.of("0xmerchant"),
        maxTimeoutSeconds = 30
    )

    private val validPayload = PaymentPayload(
        x402Version = 2,
        scheme = PaymentScheme.of("exact"),
        network = PaymentNetwork.of("base-sepolia"),
        payload = mapOf("signature" to "0xabc"),
        resource = "https://api.example.com/data",
        description = "Test resource"
    )

    @Test
    fun `no payment header returns 402 with payment required header`() {
        val handler = ServerFilters.X402PaymentRequired(fakeFacilitator()) { listOf(requirements) }
            .then { Response(OK).body("content") }

        val response = handler(Request(Method.GET, "/"))

        assertThat(response.status, equalTo(PAYMENT_REQUIRED))
        val required = paymentRequiredLens(response)
        assertThat(required.x402Version, equalTo(2))
        assertThat(required.accepts.first().scheme, equalTo(PaymentScheme.of("exact")))
    }

    @Test
    fun `invalid payment returns 402 with error`() {
        val handler = ServerFilters.X402PaymentRequired(
            fakeFacilitator(
                verifyResult = Failure(RemoteFailure(POST, Uri.of("/verify"), OK, "bad signature"))
            )
        ) { listOf(requirements) }.then { Response(OK).body("content") }

        val response = handler(Request(Method.GET, "/").with(paymentSignatureLens of validPayload))

        assertThat(response.status, equalTo(PAYMENT_REQUIRED))
        val required = paymentRequiredLens(response)
        assertThat(required.error, equalTo("bad signature"))
    }

    @Test
    fun `valid payment with successful settlement returns 200 with payment response header`() {
        val handler = ServerFilters.X402PaymentRequired(
            fakeFacilitator(
                verifyResult = Success(Verified(payer = WalletAddress.of("0xpayer"))),
                settleResult = Success(
                    Settled(
                        transaction = TransactionHash.of("0xtx"),
                        network = PaymentNetwork.of("base-sepolia"),
                        payer = WalletAddress.of("0xpayer")
                    )
                )
            )
        ) { listOf(requirements) }.then { Response(Status.OK).body("content") }

        val response = handler(Request(Method.GET, "/").with(paymentSignatureLens of validPayload))

        assertThat(response.status, equalTo(Status.OK))
        assertThat(response.bodyString(), equalTo("content"))
        val paymentResponse = paymentResponseLens(response)
        assertThat(paymentResponse.transaction, equalTo(TransactionHash.of("0xtx")))
        assertThat(paymentResponse.network, equalTo(PaymentNetwork.of("base-sepolia")))
        assertThat(paymentResponse.payer, equalTo(WalletAddress.of("0xpayer")))
    }

    @Test
    fun `settlement failure returns 402`() {
        val handler = ServerFilters.X402PaymentRequired(
            fakeFacilitator(
                verifyResult = Success(Verified(payer = WalletAddress.of("0xpayer"))),
                settleResult = Failure(RemoteFailure(POST, Uri.of("/settle"), OK, "timeout"))
            )
        ) { listOf(requirements) }.then { Response(Status.OK).body("content") }

        val response = handler(Request(Method.GET, "/").with(paymentSignatureLens of validPayload))

        assertThat(response.status, equalTo(Status.PAYMENT_REQUIRED))
        val required = paymentRequiredLens(response)
        assertThat(required.error, equalTo("timeout"))
    }

    @Suppress("UNCHECKED_CAST")
    private fun fakeFacilitator(
        verifyResult: Result<Verified, RemoteFailure> = Success(Verified(payer = WalletAddress.of("0xpayer"))),
        settleResult: Result<Settled, RemoteFailure> = Success(
            Settled(
                transaction = TransactionHash.of("0xtx"),
                network = PaymentNetwork.of("base-sepolia"),
                payer = WalletAddress.of("0xpayer")
            )
        )
    ) = object : X402Facilitator {
        override fun <R> invoke(action: X402FacilitatorAction<R>): Result<R, RemoteFailure> = when (action) {
            is Verify -> verifyResult as Result<R, RemoteFailure>
            is Settle -> settleResult as Result<R, RemoteFailure>
            else -> throw UnsupportedOperationException()
        }
    }
}
