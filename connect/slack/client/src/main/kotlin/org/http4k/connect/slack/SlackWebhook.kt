package org.http4k.connect.slack

import dev.forkhandles.result4k.Result
import org.http4k.connect.Http4kConnectApiClient
import org.http4k.connect.RemoteFailure

@Http4kConnectApiClient
interface SlackWebhook {
    suspend operator fun <R> invoke(action: SlackWebhookAction<R>): Result<R, RemoteFailure>

    companion object
}
