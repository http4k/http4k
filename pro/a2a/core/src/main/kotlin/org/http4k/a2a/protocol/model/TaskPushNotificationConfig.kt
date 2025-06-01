package org.http4k.a2a.protocol.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class TaskPushNotificationConfig(
    val taskId: String,
    val pushNotificationConfig: PushNotificationConfig
)