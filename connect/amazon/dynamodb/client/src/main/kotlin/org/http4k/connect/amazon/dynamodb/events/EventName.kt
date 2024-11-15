package org.http4k.connect.amazon.dynamodb.events

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
enum class EventName {
    INSERT, MODIFY, REMOVE
}
