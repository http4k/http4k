/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.protocol.messages

import org.http4k.ai.a2a.model.Message
import org.http4k.ai.a2a.protocol.A2ARpcMethod.Companion.of
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.PolymorphicLabel

object A2AMessage {
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
                val message: Message,
                val configuration: TaskConfiguration? = null
            )
        }

        sealed interface Response : A2AJsonRpcResponse {
            @JsonSerializable
            data class Task(
                val result: org.http4k.ai.a2a.model.Task,
                override val id: Any?,
                val jsonrpc: String = "2.0"
            ) : Response

            @JsonSerializable
            data class Message(
                val result: org.http4k.ai.a2a.model.Message,
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
                val message: Message,
                val configuration: TaskConfiguration? = null
            )
        }
    }
}
