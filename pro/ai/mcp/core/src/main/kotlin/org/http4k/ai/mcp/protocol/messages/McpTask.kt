/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.protocol.messages

import org.http4k.ai.mcp.model.Cursor
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.model.Task
import org.http4k.ai.mcp.model.TaskId
import org.http4k.ai.mcp.model.TaskStatus
import org.http4k.ai.mcp.protocol.McpRpcMethod.Companion.of
import org.http4k.connect.model.TimeToLive
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.PolymorphicLabel
import java.time.Instant

object McpTask {
    object Create {
        @JsonSerializable
        data class Response(val result: Result, override val id: Any?, val jsonrpc: String = "2.0") : McpJsonRpcResponse() {
            @JsonSerializable
            data class Result(
                val task: Task,
                override val _meta: Meta = Meta.default
            ) : HasMeta
        }
    }

    object Get : McpRpc {
        override val Method = of("tasks/get")

        @JsonSerializable
        @PolymorphicLabel("tasks/get")
        data class Request(val params: Params, override val id: Any?, val jsonrpc: String = "2.0") : McpJsonRpcRequest() {
            override val method = Get.Method

            @JsonSerializable
            data class Params(
                val taskId: TaskId,
                override val _meta: Meta = Meta.default
            ) : HasMeta
        }

        @JsonSerializable
        data class Response(val result: Result, override val id: Any?, val jsonrpc: String = "2.0") : McpJsonRpcResponse() {
            @JsonSerializable
            data class Result(
                val task: Task,
                override val _meta: Meta = Meta.default
            ) : HasMeta
        }
    }

    object Result : McpRpc {
        override val Method = of("tasks/result")

        @JsonSerializable
        @PolymorphicLabel("tasks/result")
        data class Request(val params: Params, override val id: Any?, val jsonrpc: String = "2.0") : McpJsonRpcRequest() {
            override val method = Result.Method

            @JsonSerializable
            data class Params(
                val taskId: TaskId,
                override val _meta: Meta = Meta.default
            ) : HasMeta
        }

        @JsonSerializable
        data class Response(val result: ResponseResult, override val id: Any?, val jsonrpc: String = "2.0") : McpJsonRpcResponse() {
            @JsonSerializable
            data class ResponseResult(
                val result: Map<String, Any>?,
                override val _meta: Meta = Meta.default
            ) : HasMeta
        }
    }

    object Cancel : McpRpc {
        override val Method = of("tasks/cancel")

        @JsonSerializable
        @PolymorphicLabel("tasks/cancel")
        data class Request(val params: Params, override val id: Any?, val jsonrpc: String = "2.0") : McpJsonRpcRequest() {
            override val method = Cancel.Method

            @JsonSerializable
            data class Params(
                val taskId: TaskId,
                override val _meta: Meta = Meta.default
            ) : HasMeta
        }

        @JsonSerializable
        data class Response(val result: Result, override val id: Any?, val jsonrpc: String = "2.0") : McpJsonRpcResponse() {
            @JsonSerializable
            data class Result(
                override val _meta: Meta = Meta.default
            ) : HasMeta
        }
    }

    object List : McpRpc {
        override val Method = of("tasks/list")

        @JsonSerializable
        @PolymorphicLabel("tasks/list")
        data class Request(val params: Params, override val id: Any?, val jsonrpc: String = "2.0") : McpJsonRpcRequest() {
            override val method = List.Method

            @JsonSerializable
            data class Params(
                override val cursor: Cursor? = null,
                override val _meta: Meta = Meta.default
            ) : HasMeta, PaginatedRequest
        }

        @JsonSerializable
        data class Response(val result: Result, override val id: Any?, val jsonrpc: String = "2.0") : McpJsonRpcResponse() {
            @JsonSerializable
            data class Result(
                val tasks: kotlin.collections.List<Task>,
                override val nextCursor: Cursor? = null,
                override val _meta: Meta = Meta.default
            ) : HasMeta, PaginatedResponse
        }
    }

    object Status : McpRpc {
        override val Method = of("notifications/tasks/status")

        @JsonSerializable
        @PolymorphicLabel("notifications/tasks/status")
        data class Notification(val params: Params, override val id: Any? = null, val jsonrpc: String = "2.0") : McpJsonRpcRequest() {
            override val method = Status.Method

            @JsonSerializable
            data class Params(
                val taskId: TaskId,
                val status: TaskStatus,
                val statusMessage: String? = null,
                val createdAt: Instant,
                val lastUpdatedAt: Instant,
                val ttl: TimeToLive? = null,
                val pollInterval: Int? = null,
                override val _meta: Meta = Meta.default
            ) : HasMeta {
                constructor(task: Task, meta: Meta = Meta.default) : this(
                    task.taskId, task.status, task.statusMessage,
                    task.createdAt, task.lastUpdatedAt, task.ttl, task.pollInterval, meta
                )

                fun toTask() = Task(taskId, status, statusMessage, createdAt, lastUpdatedAt, ttl, pollInterval)
            }
        }
    }
}
