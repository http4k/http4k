/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.protocol.messages

import org.http4k.ai.a2a.model.ContextId
import org.http4k.ai.a2a.model.StreamMessage
import org.http4k.ai.a2a.model.TaskId
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class A2AStream(
    val task: A2ATask? = null,
    val message: A2AMessage? = null,
    val statusUpdate: A2ATaskStatusUpdateEvent? = null,
    val artifactUpdate: A2ATaskArtifactUpdateEvent? = null
)

@JsonSerializable
data class A2ATaskStatusUpdateEvent(
    val taskId: TaskId,
    val contextId: ContextId,
    val status: A2ATaskStatus,
    val metadata: Map<String, Any>? = null
)

@JsonSerializable
data class A2ATaskArtifactUpdateEvent(
    val taskId: TaskId,
    val contextId: ContextId,
    val artifact: A2AArtifact,
    val append: Boolean? = null,
    val lastChunk: Boolean? = null,
    val metadata: Map<String, Any>? = null
)

fun A2AStream.toDomain(): StreamMessage = when {
    task != null -> StreamMessage.Task(task.toDomain())
    message != null -> StreamMessage.Message(message.toDomain())
    statusUpdate != null -> StreamMessage.StatusUpdate(statusUpdate.toDomain())
    artifactUpdate != null -> StreamMessage.ArtifactUpdate(artifactUpdate.toDomain())
    else -> error("A2AStream has no content")
}

private fun A2ATaskStatusUpdateEvent.toDomain() =
    org.http4k.ai.a2a.model.TaskStatusUpdateEvent(taskId, contextId, status.toDomain(), metadata)

private fun A2ATaskArtifactUpdateEvent.toDomain() =
    org.http4k.ai.a2a.model.TaskArtifactUpdateEvent(taskId, contextId, artifact.toDomain(), append, lastChunk, metadata)
