/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.protocol

import org.http4k.ai.a2a.MessageHandler
import org.http4k.ai.a2a.MessageRequest
import org.http4k.ai.a2a.MessageResponse
import org.http4k.ai.a2a.model.AgentCard
import org.http4k.ai.a2a.model.AgentCardProvider
import org.http4k.ai.a2a.model.ContextId
import org.http4k.ai.a2a.model.Message
import org.http4k.ai.a2a.model.PushNotificationConfig
import org.http4k.ai.a2a.model.PushNotificationConfigId
import org.http4k.ai.a2a.model.Task
import org.http4k.ai.a2a.model.TaskId
import org.http4k.ai.a2a.model.TaskPage
import org.http4k.ai.a2a.model.TaskPushNotificationConfig
import org.http4k.ai.a2a.model.TaskState
import org.http4k.ai.a2a.server.storage.PushNotificationConfigStorage
import org.http4k.ai.a2a.server.storage.TaskStorage
import org.http4k.core.Request
import java.util.UUID

class A2A(
    val cards: AgentCardProvider,
    private val tasks: TaskStorage = TaskStorage.InMemory(),
    private val pushNotifications: PushNotificationConfigStorage = PushNotificationConfigStorage.InMemory(),
    private val handler: MessageHandler
) {
    constructor(
        agentCard: AgentCard,
        tasks: TaskStorage = TaskStorage.InMemory(),
        pushNotifications: PushNotificationConfigStorage = PushNotificationConfigStorage.InMemory(),
        handler: MessageHandler
    ) : this(AgentCardProvider(agentCard), tasks, pushNotifications, handler)

    fun send(message: Message, http: Request): MessageResponse =
        handler(MessageRequest(message, http))

    fun getTask(taskId: TaskId): Task? = tasks.get(taskId)

    fun cancelTask(taskId: TaskId): Task? = tasks.cancel(taskId)

    fun listTasks(
        contextId: ContextId? = null,
        status: TaskState? = null,
        pageSize: Int? = null,
        pageToken: String? = null
    ): TaskPage = tasks.list(contextId, status, pageSize, pageToken)

    fun setPushConfig(taskId: TaskId, config: PushNotificationConfig): TaskPushNotificationConfig {
        val configId = PushNotificationConfigId.of(UUID.randomUUID().toString())
        val taskConfig = TaskPushNotificationConfig(id = configId, taskId = taskId, pushNotificationConfig = config)
        pushNotifications.store(taskConfig)
        return taskConfig
    }

    fun getPushConfig(id: PushNotificationConfigId): TaskPushNotificationConfig? =
        pushNotifications.get(id)

    fun listPushConfigs(taskId: TaskId): List<TaskPushNotificationConfig> =
        pushNotifications.list(taskId)

    fun deletePushConfig(id: PushNotificationConfigId): PushNotificationConfigId? {
        val existing = pushNotifications.get(id) ?: return null
        pushNotifications.delete(id)
        return existing.id
    }
}
