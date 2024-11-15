package org.http4k.connect.lmstudio

import dev.forkhandles.result4k.Result
import org.http4k.connect.Http4kConnectApiClient
import org.http4k.connect.RemoteFailure

@Http4kConnectApiClient
interface LmStudio {
    operator fun <R> invoke(action: LmStudioAction<R>): Result<R, RemoteFailure>

    companion object
}
