/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.protocol.messages

import org.http4k.ai.mcp.stateless.model.Meta
import org.http4k.ai.mcp.stateless.model.ResultType
import org.http4k.ai.mcp.stateless.model.TaskId
import org.http4k.ai.mcp.stateless.model.TaskStatus
import org.http4k.ai.mcp.stateless.model.TtlMs
import org.http4k.ai.mcp.stateless.protocol.McpRpcMethod.Companion.of
import org.http4k.ai.mcp.stateless.protocol.VersionedMcpEntity
import org.http4k.ai.mcp.stateless.util.McpNodeType
import org.http4k.lens.stateless.MetaKey
import org.http4k.lens.stateless.serverInfo
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.PolymorphicLabel
import java.time.Instant

object McpTask {

    @JsonSerializable
    data class Result(
        override val taskId: TaskId? = null,
        override val status: TaskStatus? = null,
        override val statusMessage: String? = null,
        override val createdAt: Instant? = null,
        override val lastUpdatedAt: Instant? = null,
        override val ttlMs: TtlMs? = null,
        override val pollIntervalMs: Long? = null,
        val result: McpNodeType? = null,
        val error: McpNodeType? = null,
        override val resultType: ResultType = ResultType.complete,
        override val inputRequests: Map<String, InputRequest>? = null,
        override val requestState: String? = null,
        override val _meta: Meta = Meta.default,
    ) : HasMeta, HasTask, HasInputRequired {
        fun withServerInfo(info: VersionedMcpEntity) =
            copy(_meta = MetaKey.serverInfo().toLens()(info, _meta))
    }

    object Get {
        @JsonSerializable
        @PolymorphicLabel("tasks/get")
        data class Request(override val params: Params, override val id: Any?, val jsonrpc: String = "2.0") :
            McpJsonRpcRequest() {
            override val method = of("tasks/get")

            @JsonSerializable
            data class Params(val taskId: TaskId, override val _meta: Meta = Meta.default) :
                HasMeta, HasMirroredName {
                override fun mirroredName() = taskId.value
            }
        }

        @JsonSerializable
        data class Response(val result: Result, override val id: Any?, val jsonrpc: String = "2.0") :
            McpJsonRpcResponse, HasServerInfo {
            override fun withServerInfo(info: VersionedMcpEntity) = copy(result = result.withServerInfo(info))
        }
    }

    object Update {
        @JsonSerializable
        @PolymorphicLabel("tasks/update")
        data class Request(override val params: Params, override val id: Any?, val jsonrpc: String = "2.0") :
            McpJsonRpcRequest() {
            override val method = of("tasks/update")

            @JsonSerializable
            data class Params(
                val taskId: TaskId,
                override val inputResponses: Map<String, McpElicitation.Result>? = null,
                override val requestState: String? = null,
                override val _meta: Meta = Meta.default
            ) : HasMeta, HasInputResponses, HasMirroredName {
                override fun mirroredName() = taskId.value
            }
        }

        @JsonSerializable
        data class Response(val result: Result, override val id: Any?, val jsonrpc: String = "2.0") :
            McpJsonRpcResponse, HasServerInfo {
            override fun withServerInfo(info: VersionedMcpEntity) = copy(result = result.withServerInfo(info))
        }
    }

    object Cancel {
        @JsonSerializable
        @PolymorphicLabel("tasks/cancel")
        data class Request(override val params: Params, override val id: Any?, val jsonrpc: String = "2.0") :
            McpJsonRpcRequest() {
            override val method = of("tasks/cancel")

            @JsonSerializable
            data class Params(val taskId: TaskId, override val _meta: Meta = Meta.default) :
                HasMeta, HasMirroredName {
                override fun mirroredName() = taskId.value
            }
        }

        @JsonSerializable
        data class Response(val result: Result, override val id: Any?, val jsonrpc: String = "2.0") :
            McpJsonRpcResponse, HasServerInfo {
            override fun withServerInfo(info: VersionedMcpEntity) = copy(result = result.withServerInfo(info))
        }
    }
}
