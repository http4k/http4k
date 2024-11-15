@file:OptIn(ExperimentalKotshiApi::class)

package org.http4k.connect.mattermost.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.PlainTextAction
import org.http4k.connect.mattermost.MattermostAction
import org.http4k.connect.mattermost.MattermostMoshi.autoBody
import org.http4k.connect.mattermost.model.Attachment
import org.http4k.connect.mattermost.model.EmojiName
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.with
import se.ansman.kotshi.ExperimentalKotshiApi
import se.ansman.kotshi.JsonProperty
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
data class TriggerWebhook(
    val key: String,
    val payload: TriggerWebhookPayload,
) : PlainTextAction(), MattermostAction<String> {
    private val lens = autoBody<TriggerWebhookPayload>().toLens()

    override fun toRequest() = Request(POST, "/hooks/$key").with(lens of payload)
}

@JsonSerializable
data class TriggerWebhookPayload(
    val text: String? = null,
    val attachments: List<Attachment>? = null,
    val channel: String? = null,
    val username: String? = null,
    @JsonProperty("icon_url")
    val iconUrl: Uri? = null,
    @JsonProperty("icon_emoji")
    val iconEmoji: EmojiName? = null,
    val type: String? = null,
    val props: Map<String, Any>? = null,
)

