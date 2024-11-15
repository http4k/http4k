package org.http4k.connect.example

import dev.forkhandles.result4k.Result
import org.http4k.connect.Http4kConnectApiClient
import org.http4k.connect.RemoteFailure

@Http4kConnectApiClient
interface Example {
    operator fun <R> invoke(action: ExampleAction<R>): Result<R, RemoteFailure>

    companion object
}
