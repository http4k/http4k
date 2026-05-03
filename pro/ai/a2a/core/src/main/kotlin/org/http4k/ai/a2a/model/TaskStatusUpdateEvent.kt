/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class TaskStatusUpdateEvent(
    val taskId: TaskId,
    val contextId: ContextId,
    val status: TaskStatus,
    val metadata: Map<String, Any>? = null
) : StreamItem
