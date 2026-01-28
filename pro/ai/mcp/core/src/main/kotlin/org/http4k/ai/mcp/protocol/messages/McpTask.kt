package org.http4k.ai.mcp.protocol.messages

import org.http4k.ai.mcp.model.Cursor
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.model.Task
import org.http4k.ai.mcp.model.TaskId
import org.http4k.ai.mcp.model.TaskStatus
import org.http4k.ai.mcp.protocol.McpRpcMethod.Companion.of
import org.http4k.connect.model.TimeToLive
import se.ansman.kotshi.JsonSerializable
import java.time.Instant

object McpTask {
    object Create {
        @JsonSerializable
        data class Response(
            val task: Task,
            override val _meta: Meta = Meta.default
        ) : ClientMessage.Response, ServerMessage.Response, HasMeta
    }

    object Get : McpRpc {
        override val Method = of("tasks/get")

        @JsonSerializable
        data class Request(
            val taskId: TaskId,
            override val _meta: Meta = Meta.default
        ) : ClientMessage.Request, ServerMessage.Request, HasMeta

        @JsonSerializable
        data class Response(
            val task: Task,
            override val _meta: Meta = Meta.default
        ) : ClientMessage.Response, ServerMessage.Response, HasMeta
    }

    object Result : McpRpc {
        override val Method = of("tasks/result")

        @JsonSerializable
        data class Request(
            val taskId: TaskId,
            override val _meta: Meta = Meta.default
        ) : ClientMessage.Request, ServerMessage.Request, HasMeta

        @JsonSerializable
        data class Response(
            val result: Map<String, Any>?,
            override val _meta: Meta = Meta.default
        ) : ClientMessage.Response, ServerMessage.Response, HasMeta
    }

    object Cancel : McpRpc {
        override val Method = of("tasks/cancel")

        @JsonSerializable
        data class Request(
            val taskId: TaskId,
            override val _meta: Meta = Meta.default
        ) : ClientMessage.Request, ServerMessage.Request, HasMeta

        @JsonSerializable
        data class Response(
            override val _meta: Meta = Meta.default
        ) : ClientMessage.Response, ServerMessage.Response, HasMeta
    }

    object List : McpRpc {
        override val Method = of("tasks/list")

        @JsonSerializable
        data class Request(
            override val cursor: Cursor? = null,
            override val _meta: Meta = Meta.default
        ) : ClientMessage.Request, ServerMessage.Request, HasMeta, PaginatedRequest

        @JsonSerializable
        data class Response(
            val tasks: kotlin.collections.List<Task>,
            override val nextCursor: Cursor? = null,
            override val _meta: Meta = Meta.default
        ) : ClientMessage.Response, ServerMessage.Response, HasMeta, PaginatedResponse
    }

    object Status : McpRpc {
        override val Method = of("notifications/tasks/status")

        @JsonSerializable
        data class Notification(
            val taskId: TaskId,
            val status: TaskStatus,
            val statusMessage: String? = null,
            val createdAt: Instant,
            val lastUpdatedAt: Instant,
            val ttl: TimeToLive? = null,
            val pollInterval: Int? = null,
            override val _meta: Meta = Meta.default
        ) : ClientMessage.Notification, ServerMessage.Notification, HasMeta {
            constructor(task: Task, meta: Meta = Meta.default) : this(
                task.taskId, task.status, task.statusMessage,
                task.createdAt, task.lastUpdatedAt, task.ttl, task.pollInterval, meta
            )

            fun toTask() = Task(taskId, status, statusMessage, createdAt, lastUpdatedAt, ttl, pollInterval)
        }
    }
}
