/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.protocol.messages

import org.http4k.ai.a2a.model.ContextId
import org.http4k.ai.a2a.model.MessageId
import org.http4k.ai.a2a.model.TaskId
import org.http4k.ai.a2a.model.Tenant
import org.http4k.ai.model.Role
import org.http4k.ai.a2a.protocol.A2ARpcMethod.Companion.of
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.PolymorphicLabel

@JsonSerializable
data class A2AMessage(
    val messageId: MessageId,
    val role: Role,
    val parts: List<A2APart>,
    val contextId: ContextId? = null,
    val taskId: TaskId? = null,
    val metadata: Map<String, Any>? = null,
    val extensions: List<String>? = null,
    val referenceTaskIds: List<String>? = null
) {
    object Send {
        @JsonSerializable
        @PolymorphicLabel("SendMessage")
        data class Request(
            val params: Params,
            override val id: Any?,
            val jsonrpc: String = "2.0"
        ) : A2AJsonRpcRequest() {
            override val method = of("SendMessage")

            @JsonSerializable
            data class Params(
                val message: A2AMessage,
                val configuration: TaskConfiguration? = null,
                val metadata: Map<String, Any>? = null,
                val tenant: Tenant? = null
            )
        }

        sealed interface Response : A2AJsonRpcResponse {
            @JsonSerializable
            data class Task(
                val result: A2ATask,
                override val id: Any?,
                val jsonrpc: String = "2.0"
            ) : Response

            @JsonSerializable
            data class Message(
                val result: A2AMessage,
                override val id: Any?,
                val jsonrpc: String = "2.0"
            ) : Response
        }
    }

    object Stream {
        @JsonSerializable
        @PolymorphicLabel("SendStreamingMessage")
        data class Request(
            val params: Params,
            override val id: Any?,
            val jsonrpc: String = "2.0"
        ) : A2AJsonRpcRequest() {
            override val method = of("SendStreamingMessage")

            @JsonSerializable
            data class Params(
                val message: A2AMessage,
                val configuration: TaskConfiguration? = null,
                val metadata: Map<String, Any>? = null,
                val tenant: Tenant? = null
            )
        }
    }
}
