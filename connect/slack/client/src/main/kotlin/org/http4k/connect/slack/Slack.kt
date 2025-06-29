package org.http4k.connect.slack

import dev.forkhandles.result4k.Result
import org.http4k.connect.Http4kConnectApiClient
import org.http4k.connect.RemoteFailure

@Http4kConnectApiClient
interface Slack {
    operator fun <R: Any> invoke(action: SlackAction<R>): Result<R, RemoteFailure>

    companion object
}


