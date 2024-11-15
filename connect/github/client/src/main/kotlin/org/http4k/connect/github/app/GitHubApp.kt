package org.http4k.connect.github.app

import dev.forkhandles.result4k.Result
import org.http4k.connect.Http4kConnectApiClient
import org.http4k.connect.RemoteFailure

@Http4kConnectApiClient
interface GitHubApp {
    operator fun <R> invoke(action: GitHubAppAction<R>): Result<R, RemoteFailure>

    companion object
}
