package org.http4k.connect.example

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.slack.Http
import org.http4k.connect.slack.Slack
import org.http4k.connect.slack.SlackWebhook
import org.http4k.connect.slack.chatPostMessage
import org.http4k.connect.slack.model.ChannelId
import org.http4k.connect.slack.model.SlackMessage
import org.http4k.connect.slack.model.SlackToken
import org.http4k.connect.slack.webhookPostMessage
import org.http4k.connect.successValue
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.filter.debug
import org.junit.jupiter.api.Test

interface SlackContract {
    val http: HttpHandler

    val channelId: ChannelId

    @Test
    fun `can post message to slack`() {
        val slack = Slack.Http({ SlackToken.of("token") }, http.debug())

        val message = SlackMessage("message", channel = channelId)
        assertThat(slack.chatPostMessage(message).successValue().channel, equalTo(channelId))

        checkMessageSentTo(message, channelId)
    }

    @Test
    fun `can post message to webhook`() {
        val webhooks = SlackWebhook.Http(Uri.of("services/a/b/c"), http.debug())

        val message = SlackMessage("message", channel = channelId)
        assertThat(webhooks.webhookPostMessage(message).successValue(), equalTo(Unit))

        checkMessageSentTo(message, channelId)
    }

    fun checkMessageSentTo(message: SlackMessage, channelId: ChannelId)
}
