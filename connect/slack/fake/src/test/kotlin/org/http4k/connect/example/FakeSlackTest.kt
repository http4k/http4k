package org.http4k.connect.example

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.hasElement
import org.http4k.connect.slack.FakeSlack
import org.http4k.connect.slack.model.ChannelId
import org.http4k.connect.slack.model.SlackMessage
import java.util.UUID

class FakeSlackTest : SlackContract {
    override val http = FakeSlack()
    override val channelId = ChannelId.of(UUID.randomUUID().toString())

    override fun checkMessageSentTo(message: SlackMessage, channelId: ChannelId) {
        assertThat(http.messages(channelId), hasElement(message))
    }
}
