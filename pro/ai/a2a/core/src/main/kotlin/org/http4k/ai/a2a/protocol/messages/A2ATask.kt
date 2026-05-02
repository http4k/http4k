/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.protocol.messages

import org.http4k.ai.a2a.model.ContextId
import org.http4k.ai.a2a.model.Cursor
import org.http4k.ai.a2a.model.TaskId
import org.http4k.ai.a2a.model.TaskState
import org.http4k.ai.a2a.model.Tenant
import org.http4k.ai.a2a.protocol.A2ARpcMethod.Companion.of
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.PolymorphicLabel

@JsonSerializable
data class A2ATask(
    val id: TaskId,
    val contextId: ContextId,
    val status: A2ATaskStatus,
    val artifacts: List<A2AArtifact>? = null,
    val history: List<A2AMessage>? = null,
    val metadata: Map<String, Any>? = null
) {
    object Get {
        @JsonSerializable
        @PolymorphicLabel("GetTask")
        data class Request(
            val params: Params,
            override val id: Any?,
            val jsonrpc: String = "2.0"
        ) : A2AJsonRpcRequest() {
            override val method = of("GetTask")

            @JsonSerializable
            data class Params(val id: TaskId, val historyLength: Int? = null, val tenant: Tenant? = null)
        }

        @JsonSerializable
        data class Response(val result: Result, override val id: Any?, val jsonrpc: String = "2.0") : A2AJsonRpcResponse {
            @JsonSerializable
            data class Result(val task: A2ATask)
        }
    }

    object Cancel {
        @JsonSerializable
        @PolymorphicLabel("CancelTask")
        data class Request(
            val params: Params,
            override val id: Any?,
            val jsonrpc: String = "2.0"
        ) : A2AJsonRpcRequest() {
            override val method = of("CancelTask")

            @JsonSerializable
            data class Params(val id: TaskId, val tenant: Tenant? = null)
        }

        @JsonSerializable
        data class Response(val result: Result, override val id: Any?, val jsonrpc: String = "2.0") : A2AJsonRpcResponse {
            @JsonSerializable
            data class Result(val task: A2ATask)
        }
    }

    object Resubscribe {
        @JsonSerializable
        @PolymorphicLabel("SubscribeToTask")
        data class Request(
            val params: Params,
            override val id: Any?,
            val jsonrpc: String = "2.0"
        ) : A2AJsonRpcRequest() {
            override val method = of("SubscribeToTask")

            @JsonSerializable
            data class Params(val id: TaskId, val tenant: Tenant? = null)
        }
    }

    object ListTasks {
        @JsonSerializable
        @PolymorphicLabel("ListTasks")
        data class Request(
            val params: Params,
            override val id: Any?,
            val jsonrpc: String = "2.0"
        ) : A2AJsonRpcRequest() {
            override val method = of("ListTasks")

            @JsonSerializable
            data class Params(
                val contextId: ContextId? = null,
                val status: TaskState? = null,
                val pageSize: Int? = null,
                val pageToken: Cursor? = null,
                val historyLength: Int? = null,
                val includeArtifacts: Boolean? = null,
                val tenant: Tenant? = null
            )
        }

        @JsonSerializable
        data class Response(val result: Result, override val id: Any?, val jsonrpc: String = "2.0") : A2AJsonRpcResponse {
            @JsonSerializable
            data class Result(
                val tasks: kotlin.collections.List<A2ATask>,
                val nextPageToken: Cursor,
                val pageSize: Int?,
                val totalSize: Int
            )
        }
    }
}
