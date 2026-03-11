package org.http4k.connect.x402.action

import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.RemoteFailure
import org.http4k.connect.x402.X402FacilitatorAction
import org.http4k.connect.x402.X402Moshi.asA
import org.http4k.connect.x402.model.SupportedResponse
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response

@Http4kConnectAction
data object Supported : X402FacilitatorAction<SupportedResponse> {
    override fun toRequest() = Request(GET, "/supported")

    override fun toResult(response: Response): Result<SupportedResponse, RemoteFailure> =
        Success(asA(response.bodyString()))
}
