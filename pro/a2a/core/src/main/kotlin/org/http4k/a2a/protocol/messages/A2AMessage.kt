package org.http4k.a2a.protocol.messages

import org.http4k.a2a.protocol.A2ARpcMethod.Companion.of
import org.http4k.a2a.protocol.model.Message
import org.http4k.a2a.protocol.model.MessageSendConfiguration
import org.http4k.a2a.protocol.model.Task
import org.http4k.a2a.protocol.model.TaskArtifactUpdateEvent
import org.http4k.a2a.protocol.model.TaskStatusUpdateEvent
import se.ansman.kotshi.JsonSerializable

object A2AMessage {
    object Send : A2ARpc {
        override val Method = of("message/send")

        @JsonSerializable
        data class Request(
            val message: Message,
            val configuration: MessageSendConfiguration? = null,
            override val metadata: Metadata = emptyMap()
        ) : ClientMessage.Request, HasMeta

        @JsonSerializable
        sealed class Response : ServerMessage.Response {
            @JsonSerializable
            data class TaskResponse(val task: Task) : Response()

            @JsonSerializable
            data class MessageResponse(val message: Message) : Response()
        }
    }

    object Stream : A2ARpc {
        override val Method = of("message/stream")

        @JsonSerializable
        data class Request(
            val message: Message,
            val configuration: MessageSendConfiguration? = null,
            override val metadata: Metadata = emptyMap()
        ) : ClientMessage.Request, HasMeta

        @JsonSerializable
        sealed class Response : ServerMessage.Response {
            @JsonSerializable
            data class TaskResponse(
                val task: Task
            ) : Response()

            @JsonSerializable
            data class MessageResponse(
                val message: Message
            ) : Response()

            @JsonSerializable
            data class StatusUpdate(
                val event: TaskStatusUpdateEvent
            ) : Response()

            @JsonSerializable
            data class ArtifactUpdate(
                val event: TaskArtifactUpdateEvent
            ) : Response()
        }
    }
}
