package org.http4k.a2a.protocol.messages

import org.http4k.a2a.protocol.model.Artifact
import org.http4k.a2a.protocol.model.SessionId
import org.http4k.a2a.protocol.model.TaskId
import org.http4k.a2a.protocol.model.TaskStatus

// Implement Part subtypes


data class Task(
    val id: TaskId,
    val sessionId: SessionId? = null,
    val status: TaskStatus,
    val artifacts: List<Artifact>? = null,
    val metadata: Metadata? = null
)

// === Error Types (Standard and A2A)

object SendStreaming {

    sealed class Update {
        data class StatusUpdated(val status: TaskStatus) : Update()
        data class ArtifactUpdated(val status: Artifact) : Update()
    }

    data class Response(
        val id: TaskId,
        val status: Update,
        val final: Boolean = false,
        val metadata: Metadata? = null
    ) : ServerMessage.Response
}
