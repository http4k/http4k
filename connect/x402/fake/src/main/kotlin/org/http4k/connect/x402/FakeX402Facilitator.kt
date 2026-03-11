package org.http4k.connect.x402

import org.http4k.chaos.ChaoticHttpHandler
import org.http4k.chaos.start
import org.http4k.connect.x402.X402Moshi.json
import org.http4k.connect.x402.model.PaymentNetwork
import org.http4k.connect.x402.model.PaymentScheme
import org.http4k.connect.x402.model.SettleResponse
import org.http4k.connect.x402.model.SupportedKind
import org.http4k.connect.x402.model.SupportedResponse
import org.http4k.connect.x402.model.TransactionHash
import org.http4k.connect.x402.model.VerifyResponse
import org.http4k.connect.x402.model.WalletAddress
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.routing.bind
import org.http4k.routing.routes

class FakeX402Facilitator(
    private val supportedSchemes: List<SupportedKind> = listOf(
        SupportedKind(PaymentScheme.of("exact"), listOf(PaymentNetwork.of("base-sepolia")))
    )
) : ChaoticHttpHandler() {
    override val app = routes(
        "/verify" bind POST to {
            Response(OK).json(
                VerifyResponse(isValid = true, payer = WalletAddress.of("0xpayer"))
            )
        },
        "/settle" bind POST to {
            Response(OK).json(
                SettleResponse(
                    success = true,
                    transaction = TransactionHash.of("0xtx"),
                    network = PaymentNetwork.of("base-sepolia"),
                    payer = WalletAddress.of("0xpayer")
                )
            )
        },
        "/supported" bind GET to {
            Response(OK).json(
                SupportedResponse(x402Version = 2, kinds = supportedSchemes)
            )
        }
    )

    fun client() = X402Facilitator.Http(Uri.of(""), app)
}

fun main() {
    FakeX402Facilitator().start()
}
