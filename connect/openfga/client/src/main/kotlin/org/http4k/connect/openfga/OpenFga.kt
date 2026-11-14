package org.http4k.connect.openfga

import dev.forkhandles.result4k.Result
import org.http4k.connect.Http4kConnectApiClient
import org.http4k.connect.RemoteFailure

@Http4kConnectApiClient
interface OpenFga {
    operator fun <R : Any> invoke(action: OpenFgaAction<R>): Result<R, RemoteFailure>

    companion object
}
