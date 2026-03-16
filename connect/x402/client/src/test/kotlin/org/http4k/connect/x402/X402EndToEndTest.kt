package org.http4k.connect.x402

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.http4k.connect.RemoteFailure
import org.http4k.connect.x402.action.Settle
import org.http4k.connect.x402.action.SettledResponse
import org.http4k.connect.x402.action.VerifiedResponse
import org.http4k.connect.x402.action.Verify
import org.http4k.connect.x402.model.AssetAddress
import org.http4k.connect.x402.model.PaymentAmount
import org.http4k.connect.x402.model.PaymentNetwork
import org.http4k.connect.x402.model.PaymentPayload
import org.http4k.connect.x402.model.PaymentRequirements
import org.http4k.connect.x402.model.PaymentScheme
import org.http4k.connect.x402.model.TransactionHash
import org.http4k.connect.x402.model.WalletAddress
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.ServerFilters
import org.http4k.filter.X402PaymentRequired
import org.http4k.lens.paymentResponseLens
import org.http4k.security.X402Security
import org.junit.jupiter.api.Test

class X402EndToEndTest {

    private val requirements = PaymentRequirements(
        scheme = PaymentScheme.of("exact"),
        network = PaymentNetwork.of("base-sepolia"),
        asset = AssetAddress.of("0xUSDC"),
        amount = PaymentAmount.of("100"),
        payTo = WalletAddress.of("0xmerchant"),
        maxTimeoutSeconds = 30
    )

    private val fakeSigner = X402Signer { reqs ->
        val req = reqs.first()
        Success(
            PaymentPayload(
                x402Version = 2,
                scheme = req.scheme,
                network = req.network,
                payload = mapOf("signature" to "0xsigned"),
                resource = "https://api.example.com/data",
                description = "Paid resource"
            )
        )
    }

    @Suppress("UNCHECKED_CAST")
    private val fakeFacilitator = object : X402Facilitator {
        override fun <R> invoke(action: X402FacilitatorAction<R>): Result<R, RemoteFailure> = when (action) {
            is Verify -> (if (action.payload.payload["signature"] == "0xsigned")
                Success(VerifiedResponse(payer = WalletAddress.of("0xpayer")))
            else
                Failure(RemoteFailure(POST, Uri.of("/verify"), OK, "invalid signature"))) as Result<R, RemoteFailure>

            is Settle -> Success(
                SettledResponse(
                    transaction = TransactionHash.of("0xtx123"),
                    network = action.requirements.network,
                    payer = WalletAddress.of("0xpayer")
                )
            ) as Result<R, RemoteFailure>

            else -> throw UnsupportedOperationException()
        }
    }

    @Test
    fun `full round trip - 402 then sign then retry then verify then settle then 200`() {
        val server = ServerFilters.X402PaymentRequired(fakeFacilitator) { listOf(requirements) }
            .then { Response(OK).body("premium content") }

        val client = ClientFilters.X402PaymentRequired(fakeSigner).then(server)

        val response = client(Request(GET, "/data"))

        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), equalTo("premium content"))
        val settleResponse = paymentResponseLens(response)
        assertThat(settleResponse.transaction, equalTo(TransactionHash.of("0xtx123")))
        assertThat(settleResponse.network, equalTo(PaymentNetwork.of("base-sepolia")))
        assertThat(settleResponse.payer, equalTo(WalletAddress.of("0xpayer")))
    }

    @Test
    fun `full round trip with security wrapper`() {
        val security = X402Security({ listOf(requirements) }, fakeFacilitator)

        val server = security.filter.then { Response(OK).body("secured content") }

        val client = ClientFilters.X402PaymentRequired(fakeSigner).then(server)

        val response = client(Request(GET, "/data"))

        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), equalTo("secured content"))
    }
}
