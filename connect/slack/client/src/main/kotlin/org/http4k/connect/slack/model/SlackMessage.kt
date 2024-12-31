package org.http4k.connect.slack.model

import org.http4k.connect.slack.SlackMoshi
import org.http4k.connect.slack.model.BlockType.header
import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class SlackMessage(
    val text: String,
    val blocks: List<Block> = emptyList(),
    val attachments: List<Attachment> = emptyList(),
    val channel: ChannelId? = null,
    val as_user: Boolean? = null,
    val icon_emoji: String? = null,
    val icon_url: Uri? = null,
    val link_names: Boolean? = null,
    val metadata: String? = null,
    val mrkdwn: Boolean? = null,
    val parse: String? = null,
    val reply_broadcast: Boolean? = null,
    val unfurl_links: Boolean? = null,
    val unfurl_media: Boolean? = null,
    val username: String? = null
) {
    companion object {
        val slackMessageBody = SlackMoshi.autoBody<SlackMessage>().toLens()
    }
}


enum class TextType {
    mrkdwn, plain_text
}

@JsonSerializable
data class Text(val text: String, val type: TextType)

enum class BlockType {
    section, header, divider
}

@JsonSerializable
data class Attachment(val text: String, val fallback: String, val color: String)

@JsonSerializable
data class Block(val text: Text?, val type: BlockType = header)
