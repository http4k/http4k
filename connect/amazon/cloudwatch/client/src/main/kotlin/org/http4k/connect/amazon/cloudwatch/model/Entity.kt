package org.http4k.connect.amazon.cloudwatch.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class Entity(
    val Attributes: Map<String, String>? = null,
    val KeyAttributes: Map<KeyAttributeKey, String>? = null,
)

