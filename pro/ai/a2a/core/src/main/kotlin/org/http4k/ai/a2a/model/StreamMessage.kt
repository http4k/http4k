/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.model

import org.http4k.ai.a2a.protocol.messages.A2AStream
import org.http4k.ai.a2a.protocol.messages.A2ATaskArtifactUpdateEvent
import org.http4k.ai.a2a.protocol.messages.A2ATaskStatusUpdateEvent
import org.http4k.ai.a2a.protocol.messages.toWire

sealed class StreamMessage {
    data class Task(val task: org.http4k.ai.a2a.model.Task) : StreamMessage()
    data class Message(val message: org.http4k.ai.a2a.model.Message) : StreamMessage()
    data class StatusUpdate(val statusUpdate: TaskStatusUpdateEvent) : StreamMessage()
    data class ArtifactUpdate(val artifactUpdate: TaskArtifactUpdateEvent) : StreamMessage()
}

fun StreamMessage.toWire(): A2AStream = when (this) {
    is StreamMessage.Task -> A2AStream(task = task.toWire())
    is StreamMessage.Message -> A2AStream(message = message.toWire())
    is StreamMessage.StatusUpdate -> A2AStream(
        statusUpdate = A2ATaskStatusUpdateEvent(statusUpdate.taskId, statusUpdate.contextId, statusUpdate.status.toWire(), statusUpdate.metadata)
    )
    is StreamMessage.ArtifactUpdate -> A2AStream(
        artifactUpdate = A2ATaskArtifactUpdateEvent(artifactUpdate.taskId, artifactUpdate.contextId, artifactUpdate.artifact.toWire(), artifactUpdate.append, artifactUpdate.lastChunk, artifactUpdate.metadata)
    )
}
