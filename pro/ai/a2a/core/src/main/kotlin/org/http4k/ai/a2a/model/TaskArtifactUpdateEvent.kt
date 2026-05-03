/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class TaskArtifactUpdateEvent(
    val taskId: TaskId,
    val contextId: ContextId,
    val artifact: Artifact,
    val append: Boolean? = null,
    val lastChunk: Boolean? = null,
    val metadata: Map<String, Any>? = null
) : StreamItem
