package org.http4k.connect.slack.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.kClass
import org.http4k.connect.slack.SlackAction
import org.http4k.connect.slack.model.ChannelId
import org.http4k.connect.slack.model.SlackMessage
import org.http4k.connect.slack.model.SlackMessage.Companion.lens
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.with
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
data class ChatPostMessage(val request: SlackMessage) : SlackAction<ChatPostMessageResponse>(kClass()) {
    override fun toRequest() = Request(POST, "/api/chat.postMessage").with(lens of request)
}

@JsonSerializable
data class ChatPostMessageResponse(val channel: ChannelId, val ts: String)
