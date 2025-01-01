package org.http4k.connect.slack

import org.http4k.connect.slack.model.ChannelId
import org.http4k.connect.slack.model.SlackMessage

interface FakeSlackState {
    fun addMessage(channelId: ChannelId, message: SlackMessage)
    fun messages(channelId: ChannelId): List<SlackMessage>
}
