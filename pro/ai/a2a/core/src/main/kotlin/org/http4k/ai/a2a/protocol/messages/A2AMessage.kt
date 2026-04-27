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
        @PolymorphicLabel("message/send")
        data class Request(
            val params: Params,
            override val id: Any?,
            val jsonrpc: String = "2.0"
        ) : A2AJsonRpcRequest() {
            override val method = of("message/send")

            @JsonSerializable
            data class Params(
                val message: Message,
                val configuration: TaskConfiguration? = null
            )
        }

        sealed interface Response {
            @JsonSerializable
            data class Task(val task: org.http4k.ai.a2a.model.Task) : Response

            @JsonSerializable
            data class Message(val message: org.http4k.ai.a2a.model.Message) : Response
        }
    }

    object Stream {
        @JsonSerializable
        @PolymorphicLabel("message/stream")
        data class Request(
            val params: Params,
            override val id: Any?,
            val jsonrpc: String = "2.0"
        ) : A2AJsonRpcRequest() {
            override val method = of("message/stream")

            @JsonSerializable
            data class Params(
                val message: Message,
                val configuration: TaskConfiguration? = null
            )
        }
    }
}
