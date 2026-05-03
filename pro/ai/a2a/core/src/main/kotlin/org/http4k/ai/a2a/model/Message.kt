/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class Message(
    val messageId: MessageId,
    val role: A2ARole,
    val parts: List<Part>,
    val contextId: ContextId? = null,
    val taskId: TaskId? = null,
    val metadata: Map<String, Any>? = null,
    val extensions: List<String>? = null,
    val referenceTaskIds: List<TaskId>? = null
) : StreamItem, MessageResponse
