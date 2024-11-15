package org.http4k.connect.mattermost.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class AttachmentField(
    val title: String,
    val value: String,
    val short: Boolean? = null,
)
