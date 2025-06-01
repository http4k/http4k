package org.http4k.a2a.protocol.model

import org.http4k.a2a.protocol.messages.Metadata
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class Message(
    val messageId: String,
    val role: MessageRole,
    val parts: List<Part>,
    val contextId: ContextId? = null,
    val taskId: TaskId? = null,
    val referenceTaskIds: List<TaskId>? = null,
    val metadata: Metadata = emptyMap()
)

