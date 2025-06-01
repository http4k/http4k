package org.http4k.a2a.protocol.messages

import org.http4k.a2a.protocol.A2ARpcMethod.Companion.of
import org.http4k.a2a.protocol.model.Task
import org.http4k.a2a.protocol.model.TaskId
import org.http4k.a2a.protocol.model.TaskPushNotificationConfig
import se.ansman.kotshi.JsonSerializable

/**
 * Task-related operations for A2A protocol
 */
object A2ATask {

    object Get : A2ARpc {
        override val Method = of("tasks/get")

        @JsonSerializable
        data class Request(
            val id: TaskId,
            val historyLength: Int? = null,
            override val metadata: Metadata = emptyMap()
        ) : ClientMessage.Request, HasMeta

        @JsonSerializable
        data class Response(
            val task: Task
        ) : ServerMessage.Response
    }

    object Cancel : A2ARpc {
        override val Method = of("tasks/cancel")

        @JsonSerializable
        data class Request(
            val id: TaskId,
            override val metadata: Metadata = emptyMap()
        ) : ClientMessage.Request, HasMeta

        @JsonSerializable
        data class Response(
            val task: Task
        ) : ServerMessage.Response
    }

    object PushNotificationConfig {

        object Set : A2ARpc {
            override val Method = of("tasks/pushNotificationConfig/set")

            @JsonSerializable
            data class Request(
                val taskId: TaskId,
                val pushNotificationConfig: org.http4k.a2a.protocol.model.PushNotificationConfig
            ) : ClientMessage.Request

            @JsonSerializable
            data class Response(
                val taskPushNotificationConfig: TaskPushNotificationConfig
            ) : ServerMessage.Response
        }

        object Get : A2ARpc {
            override val Method = of("tasks/pushNotificationConfig/get")

            @JsonSerializable
            data class Request(
                val id: TaskId,
                override val metadata: Metadata = emptyMap()
            ) : ClientMessage.Request, HasMeta

            @JsonSerializable
            data class Response(
                val taskPushNotificationConfig: TaskPushNotificationConfig
            ) : ServerMessage.Response
        }
    }

    object Resubscribe : A2ARpc {
        override val Method = of("tasks/resubscribe")

        @JsonSerializable
        data class Request(
            val id: TaskId,
            override val metadata: Metadata = emptyMap()
        ) : ClientMessage.Request, HasMeta

        // Note: Response is the same as Stream response - returns streaming events
    }
}
