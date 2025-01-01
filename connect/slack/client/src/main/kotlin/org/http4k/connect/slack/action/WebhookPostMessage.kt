package org.http4k.connect.slack.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.RemoteFailure
import org.http4k.connect.slack.SlackWebhookAction
import org.http4k.connect.slack.model.SlackMessage
import org.http4k.connect.slack.model.SlackMessage.Companion.slackMessageBody
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.with

@Http4kConnectAction
data class WebhookPostMessage(val request: SlackMessage) : SlackWebhookAction<Unit> {
    override fun toRequest() = Request(POST, "").with(slackMessageBody of request)

    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> Success(Unit)
            else -> Failure(RemoteFailure(toRequest().method, toRequest().uri, status, bodyString()))
        }
    }
}
