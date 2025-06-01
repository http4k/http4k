package org.http4k.a2a.protocol.model

import org.http4k.a2a.protocol.messages.Metadata
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.PolymorphicLabel

@JsonSerializable
@PolymorphicLabel("status-update")
data class TaskStatusUpdateEvent(
    val taskId: TaskId,
    val contextId: ContextId,
    val status: TaskStatus,
    val final: Boolean,
    val metadata: Metadata = emptyMap()
) : A2AEvent
