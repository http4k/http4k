package org.http4k.connect.github.api

import dev.forkhandles.result4k.Result
import org.http4k.connect.Http4kConnectApiClient
import org.http4k.connect.RemoteFailure

@Http4kConnectApiClient
interface GitHub {
    operator fun <R> invoke(action: GitHubAction<R>): Result<R, RemoteFailure>

    companion object
}

