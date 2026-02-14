package org.http4k.ai.a2a.protocol.messages

import org.http4k.ai.a2a.model.PushNotificationConfig
import org.http4k.ai.a2a.model.PushNotificationConfigId
import org.http4k.ai.a2a.model.TaskId
import org.http4k.ai.a2a.model.TaskPushNotificationConfig
import org.http4k.ai.a2a.protocol.A2ARpcMethod.Companion.of
import se.ansman.kotshi.JsonSerializable

object A2APushNotificationConfig {
    object Set : A2ARpc {
        override val Method = of("tasks/pushNotificationConfig/set")

        @JsonSerializable
        data class Request(
            val taskId: TaskId,
            val pushNotificationConfig: PushNotificationConfig
        ) : A2ARequest

        @JsonSerializable
        data class Response(
            val id: PushNotificationConfigId,
            val taskId: TaskId,
            val pushNotificationConfig: PushNotificationConfig
        ) : A2AResponse
    }

    object Get : A2ARpc {
        override val Method = of("tasks/pushNotificationConfig/get")

        @JsonSerializable
        data class Request(val id: PushNotificationConfigId) : A2ARequest

        @JsonSerializable
        data class Response(
            val id: PushNotificationConfigId,
            val taskId: TaskId,
            val pushNotificationConfig: PushNotificationConfig
        ) : A2AResponse
    }

    object List : A2ARpc {
        override val Method = of("tasks/pushNotificationConfig/list")

        @JsonSerializable
        data class Request(val taskId: TaskId) : A2ARequest

        @JsonSerializable
        data class Response(val configs: kotlin.collections.List<TaskPushNotificationConfig>) : A2AResponse
    }

    object Delete : A2ARpc {
        override val Method = of("tasks/pushNotificationConfig/delete")

        @JsonSerializable
        data class Request(val id: PushNotificationConfigId) : A2ARequest

        @JsonSerializable
        data class Response(val id: PushNotificationConfigId) : A2AResponse
    }
}
