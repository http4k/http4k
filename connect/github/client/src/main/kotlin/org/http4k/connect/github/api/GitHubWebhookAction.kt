package org.http4k.connect.github.api

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.http4k.connect.Action
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.RemoteFailure
import org.http4k.connect.asRemoteFailure
import org.http4k.connect.github.webhook.WebhookEventType
import org.http4k.core.ContentType
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.format.AutoMarshalling
import org.http4k.format.Moshi
import org.http4k.lens.GITHUB_JSON
import org.http4k.lens.Header
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.lens.X_GITHUB_EVENT

@Http4kConnectAction
abstract class GitHubWebhookAction(
    private val event: WebhookEventType,
    private val autoMarshalling: AutoMarshalling = Moshi
) : Action<Result<Unit, RemoteFailure>> {
    override fun toRequest() = Request(POST, Uri.of(""))
        .with(CONTENT_TYPE of ContentType.GITHUB_JSON, Header.X_GITHUB_EVENT of event)
        .body(autoMarshalling.asFormatString(this))

    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> Success(Unit)
            else -> Failure(asRemoteFailure(this))
        }
    }
}

