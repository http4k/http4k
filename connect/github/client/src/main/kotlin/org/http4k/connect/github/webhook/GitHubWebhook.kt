package org.http4k.connect.github.webhook

import dev.forkhandles.result4k.Result
import org.http4k.connect.Http4kConnectApiClient
import org.http4k.connect.RemoteFailure
import org.http4k.connect.github.api.GitHubWebhookAction

@Http4kConnectApiClient
fun interface GitHubWebhook {
    operator fun invoke(action: GitHubWebhookAction): Result<Unit, RemoteFailure>

    companion object
}

