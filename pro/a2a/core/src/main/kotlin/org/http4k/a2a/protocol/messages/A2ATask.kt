package org.http4k.a2a.protocol.messages

import org.http4k.a2a.protocol.A2ARpcMethod
import org.http4k.a2a.protocol.model.Artifact
import org.http4k.a2a.protocol.model.Message
import org.http4k.a2a.protocol.model.PushNotificationConfig
import org.http4k.a2a.protocol.model.SessionId
import org.http4k.a2a.protocol.model.TaskId
import org.http4k.a2a.protocol.model.TaskStatus

object A2ATask {
    object Send : A2ARpc {
        override val Method = A2ARpcMethod.of("tasks/send")

        data class Request(
            val id: TaskId,
            val sessionId: SessionId? = null,
            val message: Message,
            val pushNotification: PushNotificationConfig? = null,
            val historyLength: Int? = null,
            override val metadata: Metadata? = null
        ) : ClientMessage.Request

        data class Response(
            val id: TaskId,
            val sessionId: SessionId? = null,
            val status: TaskStatus,
            val artifacts: List<Artifact>? = null,
            val metadata: Metadata? = null
        ) : ClientMessage.Response
    }

    object Get : A2ARpc {
        override val Method = A2ARpcMethod.of("tasks/send")

        data class Request(val id: TaskId, val historyLength: Int? = null, override val metadata: Metadata? = null) :
            ClientMessage.Request

        data class Response(
            val id: TaskId,
            val sessionId: SessionId? = null,
            val status: TaskStatus,
            val artifacts: List<Artifact>? = null,
            val metadata: Metadata? = null
        ) : ClientMessage.Response
    }

    object Cancel : A2ARpc {
        override val Method = A2ARpcMethod.of("tasks/cancel")

        data class Request(val id: TaskId, val historyLength: Int? = null, override val metadata: Metadata? = null) :
            ClientMessage.Request

        data class Response(
            val id: TaskId,
            val sessionId: SessionId? = null,
            val status: TaskStatus,
            val artifacts: List<Artifact>? = null,
            val metadata: Metadata? = null
        ) : ClientMessage.Response
    }

    object PushNotifications {
        object Set : A2ARpc {
            override val Method = A2ARpcMethod.of("tasks/push_notifications/set")

            data class Request(val id: TaskId, val pushNotificationConfig: PushNotificationConfig) :
                ClientMessage.Request

            data class Response(val id: TaskId, val pushNotificationConfig: PushNotificationConfig) :
                ClientMessage.Response
        }

        object Get : A2ARpc {
            override val Method = A2ARpcMethod.of("tasks/push_notifications/get")

            data class Request(val id: TaskId, override val metadata: Metadata? = null) : ClientMessage.Request

            data class Response(val id: TaskId, val pushNotificationConfig: PushNotificationConfig) :
                ClientMessage.Response
        }
    }

    object Resubscribe : A2ARpc {
        override val Method = A2ARpcMethod.of("tasks/resubscribe")

        data class Request(val id: TaskId, val historyLength: Int? = null, override val metadata: Metadata? = null) :
            ClientMessage.Request
    }

    object Subscribe : A2ARpc {
        override val Method = A2ARpcMethod.of("tasks/sendSubscribe")

        data class Request(
            val id: TaskId,
            val sessionId: SessionId? = null,
            val message: Message,
            val pushNotification: PushNotificationConfig? = null,
            val historyLength: Int? = null,
            override val metadata: Metadata? = null
        ) : ClientMessage.Request
    }

    object History : A2ARpc {
        override val Method = A2ARpcMethod.of("tasks/getHistory")

        data class Request(val id: TaskId, val historyLength: Int? = null, override val metadata: Metadata? = null) :
            ClientMessage.Request

        data class Response(val messageHistory: List<Message> = emptyList()) : ClientMessage.Response
    }
}


