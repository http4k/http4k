package org.http4k.connect.amazon.cloudwatch.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
enum class ActionsSuppressedBy {
    WaitPeriod,
    ExtensionPeriod,
    Alarm,
}
