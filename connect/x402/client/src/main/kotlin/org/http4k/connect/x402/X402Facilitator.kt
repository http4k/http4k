package org.http4k.connect.x402

import dev.forkhandles.result4k.Result
import org.http4k.connect.Http4kConnectApiClient
import org.http4k.connect.RemoteFailure

@Http4kConnectApiClient
interface X402Facilitator {
    operator fun <R> invoke(action: X402FacilitatorAction<R>): Result<R, RemoteFailure>
    companion object
}
