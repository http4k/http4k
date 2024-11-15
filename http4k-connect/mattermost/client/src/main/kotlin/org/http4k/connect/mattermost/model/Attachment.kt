@file:OptIn(ExperimentalKotshiApi::class)

package org.http4k.connect.mattermost.model

import org.http4k.core.Uri
import se.ansman.kotshi.ExperimentalKotshiApi
import se.ansman.kotshi.JsonProperty
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class Attachment(
    val fallback: String,
    val color: String? = null,
    val pretext: String? = null,
    val text: String,
    @JsonProperty("author_name")
    val authorName: String? = null,
    @JsonProperty("author_link")
    val authorLink: Uri? = null,
    @JsonProperty("author_icon")
    val authorIcon: Uri? = null,
    val title: String? = null,
    @JsonProperty("title_link")
    val titleLink: Uri? = null,
    val fields: List<AttachmentField>? = null,
    @JsonProperty("image_url")
    val imageUrl: Uri? = null,
    @JsonProperty("thumb_url")
    val thumbUrl: Uri? = null,
    val footer: String? = null,
    @JsonProperty("footer_icon")
    val footerIcon: String? = null,
    val actions: List<PostAction>? = null,
)
