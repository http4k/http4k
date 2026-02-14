package org.http4k.ai.a2a.protocol.messages

import org.http4k.ai.a2a.model.ContextId
import org.http4k.ai.a2a.model.Cursor
import org.http4k.ai.a2a.model.Task
import org.http4k.ai.a2a.model.TaskId
import org.http4k.ai.a2a.model.TaskState
import org.http4k.ai.a2a.protocol.A2ARpcMethod.Companion.of
import se.ansman.kotshi.JsonSerializable

object A2ATask {
    object Get : A2ARpc {
        override val Method = of("tasks/get")

        @JsonSerializable
        data class Request(val id: TaskId, val historyLength: Int? = null) : A2ARequest

        @JsonSerializable
        data class Response(val task: Task) : A2AResponse
    }

    object Cancel : A2ARpc {
        override val Method = of("tasks/cancel")

        @JsonSerializable
        data class Request(val id: TaskId) : A2ARequest

        @JsonSerializable
        data class Response(val task: Task) : A2AResponse
    }

    object Resubscribe : A2ARpc {
        override val Method = of("tasks/resubscribe")

        @JsonSerializable
        data class Request(val id: TaskId) : A2ARequest
    }

    object List : A2ARpc {
        override val Method = of("tasks/list")

        @JsonSerializable
        data class Request(
            val contextId: ContextId? = null,
            val status: TaskState? = null,
            val pageSize: Int? = null,
            val pageToken: Cursor? = null,
            val historyLength: Int? = null,
            val includeArtifacts: Boolean? = null
        ) : A2ARequest

        @JsonSerializable
        data class Response(
            val tasks: kotlin.collections.List<Task>,
            val nextPageToken: Cursor,
            val pageSize: Int?,
            val totalSize: Int
        ) : A2AResponse
    }
}
