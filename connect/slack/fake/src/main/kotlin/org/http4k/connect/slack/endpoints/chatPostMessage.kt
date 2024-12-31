package org.http4k.connect.slack.endpoints

import org.http4k.connect.slack.FakeSlackState
import org.http4k.connect.slack.SlackMoshi
import org.http4k.connect.slack.model.SlackMessage.Companion.slackMessageBody
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.routing.bind

fun chatPostMessage(state: FakeSlackState) =
    "/api/chat.postMessage" bind POST to { req: Request ->
        val messageRequest = slackMessageBody(req)

        state.addMessage(messageRequest.channel!!, messageRequest)
        Response(OK).with(CONTENT_TYPE of APPLICATION_JSON).body(
            """{
    "ok": true,
    "channel": "${messageRequest.channel}",
    "ts": "1503435956.000247",
    "message": {
        "text": ${SlackMoshi.asFormatString(messageRequest.text)},
        "username": "ecto1",
        "bot_id": "B19LU7CSY",
        "attachments": [
            {
                "text": "This is an attachment",
                "id": 1,
                "fallback": "This is an attachment's fallback"
            }
        ],
        "type": "message",
        "subtype": "bot_message",
        "ts": "1503435956.000247"
    }
}""".trimIndent()
        )

    }
