package org.http4k.a2a.protocol.model

import org.http4k.a2a.protocol.messages.Metadata
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.PolymorphicLabel

@JsonSerializable
@PolymorphicLabel("artifact-update")
data class TaskArtifactUpdateEvent(
    val taskId: TaskId,
    val contextId: ContextId,
    val artifact: Artifact,
    val append: Boolean? = null,
    val lastChunk: Boolean? = null,
    val metadata: Metadata = emptyMap()
) : A2AEvent
