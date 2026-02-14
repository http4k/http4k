package org.http4k.ai.a2a.protocol.messages

import org.http4k.ai.a2a.model.Message
import org.http4k.ai.a2a.protocol.A2ARpcMethod.Companion.of
import se.ansman.kotshi.JsonSerializable

object A2AMessage {
    object Send : A2ARpc {
        override val Method = of("message/send")

        @JsonSerializable
        data class Request(
            val message: Message,
            val configuration: TaskConfiguration? = null
        ) : A2ARequest

        sealed interface Response : A2AResponse {
            @JsonSerializable
            data class Task(val task: org.http4k.ai.a2a.model.Task) : Response

            @JsonSerializable
            data class Message(val message: org.http4k.ai.a2a.model.Message) : Response
        }
    }

    object Stream : A2ARpc {
        override val Method = of("message/stream")

        @JsonSerializable
        data class Request(
            val message: Message,
            val configuration: TaskConfiguration? = null
        ) : A2ARequest
    }
}
