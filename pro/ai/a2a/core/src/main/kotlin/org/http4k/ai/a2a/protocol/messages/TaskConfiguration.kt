/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.protocol.messages

import org.http4k.ai.a2a.model.TaskPushNotificationConfig
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class TaskConfiguration(
    val acceptedOutputModes: List<String>? = null,
    val historyLength: Int? = null,
    val taskPushNotificationConfig: TaskPushNotificationConfig? = null,
    val returnImmediately: Boolean? = null
)
