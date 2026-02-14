package org.http4k.ai.a2a.client

import org.http4k.ai.a2a.A2AResult
import org.http4k.ai.a2a.model.AgentCard
import org.http4k.ai.a2a.model.Message
import org.http4k.ai.a2a.model.PushNotificationConfigId
import org.http4k.ai.a2a.model.Task
import org.http4k.ai.a2a.model.TaskId
import org.http4k.ai.a2a.protocol.messages.A2AMessage
import org.http4k.ai.a2a.protocol.messages.A2ATask
import org.http4k.ai.a2a.model.PushNotificationConfig
import org.http4k.ai.a2a.model.TaskPushNotificationConfig

interface A2AClient : AutoCloseable {
    fun agentCard(): A2AResult<AgentCard>
    fun message(message: Message): A2AResult<A2AMessage.Send.Response>
    fun messageStream(message: Message): A2AResult<Sequence<A2AMessage.Send.Response>>
    fun tasks(): Tasks
    fun pushNotificationConfigs(): PushNotificationConfigs

    interface Tasks {
        fun get(taskId: TaskId): A2AResult<Task>
        fun cancel(taskId: TaskId): A2AResult<Task>
        fun list(request: A2ATask.List.Request = A2ATask.List.Request()): A2AResult<A2ATask.List.Response>
    }

    interface PushNotificationConfigs {
        fun set(taskId: TaskId, config: PushNotificationConfig): A2AResult<TaskPushNotificationConfig>
        fun get(id: PushNotificationConfigId): A2AResult<TaskPushNotificationConfig>
        fun list(taskId: TaskId): A2AResult<List<TaskPushNotificationConfig>>
        fun delete(id: PushNotificationConfigId): A2AResult<PushNotificationConfigId>
    }

    companion object
}
