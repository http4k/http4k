package org.http4k.connect.amazon.cloudwatch.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
enum class KeyAttributeKey {
    Type,
    ResourceType,
    Identifier,
    Name,
    Environment,
}
