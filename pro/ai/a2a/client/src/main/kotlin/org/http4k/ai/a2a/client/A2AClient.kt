/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.client

import org.http4k.ai.a2a.A2AResult
import org.http4k.ai.a2a.MessageResponse
import org.http4k.ai.a2a.model.AgentCard
import org.http4k.ai.a2a.model.ContextId
import org.http4k.ai.a2a.model.Message
import org.http4k.ai.a2a.model.PushNotificationConfig
import org.http4k.ai.a2a.model.PushNotificationConfigId
import org.http4k.ai.a2a.model.Task
import org.http4k.ai.a2a.model.TaskId
import org.http4k.ai.a2a.model.TaskPage
import org.http4k.ai.a2a.model.TaskPushNotificationConfig
import org.http4k.ai.a2a.model.TaskState
import org.http4k.ai.a2a.protocol.messages.TaskConfiguration

interface A2AClient : AutoCloseable {
    fun agentCard(): A2AResult<AgentCard>
    fun extendedAgentCard(): A2AResult<AgentCard>
    fun message(message: Message, configuration: TaskConfiguration? = null): A2AResult<MessageResponse>
    fun messageStream(message: Message, configuration: TaskConfiguration? = null): A2AResult<Sequence<MessageResponse>>
    fun tasks(): Tasks
    fun pushNotificationConfigs(): PushNotificationConfigs

    interface Tasks {
        fun get(taskId: TaskId): A2AResult<Task>
        fun cancel(taskId: TaskId): A2AResult<Task>
        fun list(
            contextId: ContextId? = null,
            status: TaskState? = null,
            pageSize: Int? = null,
            pageToken: String? = null
        ): A2AResult<TaskPage>
    }

    interface PushNotificationConfigs {
        fun set(taskId: TaskId, config: PushNotificationConfig): A2AResult<TaskPushNotificationConfig>
        fun get(id: PushNotificationConfigId): A2AResult<TaskPushNotificationConfig>
        fun list(taskId: TaskId): A2AResult<List<TaskPushNotificationConfig>>
        fun delete(id: PushNotificationConfigId): A2AResult<PushNotificationConfigId>
    }
}
