package org.http4k.ai.a2a.protocol.messages

import org.http4k.ai.a2a.model.PushNotificationConfig
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class TaskConfiguration(
    val acceptedOutputModes: List<String>? = null,
    val historyLength: Int? = null,
    val pushNotificationConfig: PushNotificationConfig? = null
)
