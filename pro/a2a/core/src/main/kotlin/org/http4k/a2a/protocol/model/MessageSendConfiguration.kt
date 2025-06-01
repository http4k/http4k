package org.http4k.a2a.protocol.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class MessageSendConfiguration(
    val acceptedOutputModes: List<String>,
    val blocking: Boolean? = null,
    val historyLength: Int? = null,
    val pushNotificationConfig: PushNotificationConfig? = null
)