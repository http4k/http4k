package org.http4k.connect.slack

import org.http4k.chaos.ChaoticHttpHandler
import org.http4k.chaos.start
import org.http4k.connect.slack.endpoints.chatPostMessage
import org.http4k.connect.slack.endpoints.webhookPostMessage
import org.http4k.connect.slack.model.ChannelId
import org.http4k.connect.slack.model.SlackMessage
import org.http4k.connect.slack.model.SlackToken
import org.http4k.routing.routes
import java.util.concurrent.ConcurrentHashMap

class FakeSlack : ChaoticHttpHandler(), FakeSlackState {
    private val messages = ConcurrentHashMap<ChannelId, MutableList<SlackMessage>>()

    override fun addMessage(channelId: ChannelId, message: SlackMessage) {
        messages.compute(channelId) { _, value -> (value ?: mutableListOf()).apply { add(message) } }
    }

    override fun messages(channelId: ChannelId) =
        messages.getOrDefault(channelId, emptyList())

    fun client() = Slack.Http({ SlackToken.of("test") }, this)

    override val app = routes(
        chatPostMessage(this),
        webhookPostMessage(this)
    )
}

fun main() {
    FakeSlack().start()
}
