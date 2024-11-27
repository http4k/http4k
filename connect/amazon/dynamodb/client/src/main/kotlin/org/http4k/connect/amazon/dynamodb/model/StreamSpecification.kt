package org.http4k.connect.amazon.dynamodb.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class StreamSpecification(
    val StreamEnabled: Boolean,
    val StreamViewType: StreamViewType? = null
)
