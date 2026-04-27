/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.model

import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class PushNotificationConfig(
    val url: Uri,
    val token: String? = null,
    val authentication: AgentAuthentication? = null
)

@JsonSerializable
data class TaskPushNotificationConfig(
    val id: PushNotificationConfigId,
    val taskId: TaskId,
    val pushNotificationConfig: PushNotificationConfig
)
