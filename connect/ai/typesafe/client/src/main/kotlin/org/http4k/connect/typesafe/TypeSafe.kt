package org.http4k.connect.typesafe

import dev.forkhandles.result4k.Result
import org.http4k.connect.Http4kConnectApiClient
import org.http4k.connect.RemoteFailure

@Http4kConnectApiClient
interface TypeSafe {
    operator fun <R> invoke(action: TypeSafeAction<R>): Result<R, RemoteFailure>

    companion object
}
