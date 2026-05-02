/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.protocol.messages

import org.http4k.ai.a2a.model.ContextId
import org.http4k.ai.a2a.model.Cursor
import org.http4k.ai.a2a.model.Task
import org.http4k.ai.a2a.model.TaskId
import org.http4k.ai.a2a.model.TaskState
import org.http4k.ai.a2a.protocol.A2ARpcMethod.Companion.of
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.PolymorphicLabel

object A2ATask {
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
            data class Params(val id: TaskId, val historyLength: Int? = null)
        }

        @JsonSerializable
        data class Response(val result: Result, override val id: Any?, val jsonrpc: String = "2.0") : A2AJsonRpcResponse {
            @JsonSerializable
            data class Result(val task: Task)
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
            data class Params(val id: TaskId)
        }

        @JsonSerializable
        data class Response(val result: Result, override val id: Any?, val jsonrpc: String = "2.0") : A2AJsonRpcResponse {
            @JsonSerializable
            data class Result(val task: Task)
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
            data class Params(val id: TaskId)
        }
    }

    object List {
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
                val includeArtifacts: Boolean? = null
            )
        }

        @JsonSerializable
        data class Response(val result: Result, override val id: Any?, val jsonrpc: String = "2.0") : A2AJsonRpcResponse {
            @JsonSerializable
            data class Result(
                val tasks: kotlin.collections.List<Task>,
                val nextPageToken: Cursor,
                val pageSize: Int?,
                val totalSize: Int
            )
        }
    }
}
