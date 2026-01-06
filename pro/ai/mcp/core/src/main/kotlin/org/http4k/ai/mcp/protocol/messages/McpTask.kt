package org.http4k.ai.mcp.protocol.messages

import org.http4k.ai.mcp.model.Cursor
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.model.Task
import org.http4k.ai.mcp.model.TaskId
import org.http4k.ai.mcp.protocol.McpRpcMethod.Companion.of
import se.ansman.kotshi.JsonSerializable

object McpTask {
    object Get : McpRpc {
        override val Method = of("tasks/get")

        @JsonSerializable
        data class Request(
            val taskId: TaskId,
            override val _meta: Meta = Meta.default
        ) : ClientMessage.Request, ServerMessage.Request, HasMeta {
            override val task = null
        }

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
        ) : ClientMessage.Request, ServerMessage.Request, HasMeta {
            override val task = null
        }

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
        ) : ClientMessage.Request, ServerMessage.Request, HasMeta {
            override val task = null
        }

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
        ) : ClientMessage.Request, ServerMessage.Request, HasMeta, PaginatedRequest {
            override val task = null
        }

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
            val task: Task
        ) : ClientMessage.Notification, ServerMessage.Notification
    }
}
