package org.http4k.connect.x402.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.RemoteFailure
import org.http4k.connect.x402.X402FacilitatorAction
import org.http4k.connect.x402.X402Moshi.json
import org.http4k.connect.x402.model.FacilitatorRequest
import org.http4k.connect.x402.model.PaymentPayload
import org.http4k.connect.x402.model.PaymentRequirements
import org.http4k.connect.x402.model.VerifyResponse
import org.http4k.connect.x402.model.WalletAddress
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
data class Verify(val payload: PaymentPayload, val requirements: PaymentRequirements) : X402FacilitatorAction<VerifiedResponse> {
    override fun toRequest() = Request(POST, "/verify").json(FacilitatorRequest(payload, requirements))

    override fun toResult(response: Response): Result<VerifiedResponse, RemoteFailure> {
        val wire = response.json<VerifyResponse>()
        return when {
            wire.isValid -> Success(VerifiedResponse(wire.payer!!))
            else -> Failure(RemoteFailure(POST, Uri.of("/verify"), response.status, wire.invalidReason))
        }
    }
}

@JsonSerializable
data class VerifiedResponse(val payer: WalletAddress)
