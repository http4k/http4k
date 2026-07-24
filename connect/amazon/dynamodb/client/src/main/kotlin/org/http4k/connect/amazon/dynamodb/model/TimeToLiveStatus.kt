package org.http4k.connect.amazon.dynamodb.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
enum class TimeToLiveStatus {
    ENABLING, DISABLING, ENABLED, DISABLED
}
