package org.http4k.ai.a2a.model

import org.http4k.ai.a2a.model.Task.Kind.task
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class Task(
    val id: TaskId,
    val contextId: ContextId,
    val status: TaskStatus,
    val artifacts: List<Artifact>? = null,
    val history: List<Message>? = null,
    val metadata: Map<String, Any>? = null,
    val kind: Kind = task
) {
    enum class Kind {
        task
    }
}
