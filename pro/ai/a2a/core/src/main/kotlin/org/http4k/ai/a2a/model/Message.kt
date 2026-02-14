package org.http4k.ai.a2a.model

import org.http4k.ai.a2a.model.Message.Kind.message
import org.http4k.ai.model.Role
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class Message(
    val role: Role,
    val parts: List<Part>,
    val messageId: MessageId? = null,
    val contextId: ContextId? = null,
    val taskId: TaskId? = null,
    val metadata: Map<String, Any>? = null,
    val kind: Kind = message
) {
    enum class Kind {
        message
    }
}
