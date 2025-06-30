package org.http4k.connect.amazon.cloudwatch.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class MessageData(
    val Code: String? = null,
    val Value: String? = null,
)
