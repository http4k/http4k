package org.http4k.a2a.protocol.model

import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.Polymorphic
import se.ansman.kotshi.PolymorphicLabel

@JsonSerializable
@Polymorphic("kind")
sealed interface A2AEvent

@PolymorphicLabel("message")
@JsonSerializable
data class MessageEvent(
    val messageId: String,
    val role: MessageRole,
    val parts: List<Part>,
    val contextId: ContextId? = null,
    val taskId: String? = null,
    val referenceTaskIds: List<String>? = null,
    val metadata: Metadata = emptyMap()
) : A2AEvent

@PolymorphicLabel("task")
@JsonSerializable
data class TaskEvent(
    val id: TaskId,
    val contextId: ContextId,
    val status: TaskStatus,
    val history: List<Message>? = null,
    val artifacts: List<Artifact>? = null,
    val metadata: Metadata = emptyMap()
) : A2AEvent
