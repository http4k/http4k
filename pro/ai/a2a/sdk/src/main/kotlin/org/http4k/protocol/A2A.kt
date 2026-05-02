/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.protocol

import org.http4k.ai.a2a.MessageHandler
import org.http4k.ai.a2a.protocol.messages.toDomain
import org.http4k.ai.a2a.MessageRequest
import org.http4k.ai.a2a.MessageResponse
import org.http4k.ai.a2a.model.AgentCard
import org.http4k.ai.a2a.model.AgentCardProvider
import org.http4k.ai.a2a.model.PushNotificationConfigId
import org.http4k.ai.a2a.protocol.messages.toDomain
import org.http4k.ai.a2a.model.StreamMessage
import org.http4k.ai.a2a.model.TaskPushNotificationConfig
import org.http4k.ai.a2a.protocol.messages.A2AMessage
import org.http4k.ai.a2a.protocol.messages.A2APushNotificationConfig
import org.http4k.ai.a2a.protocol.messages.A2ATask
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

    fun send(params: A2AMessage.Send.Request.Params, http: Request): MessageResponse =
        handler(MessageRequest(params.message.toDomain(), http))

    fun stream(params: A2AMessage.Stream.Request.Params, http: Request): Sequence<StreamMessage> {
        val response = handler(MessageRequest(params.message.toDomain(), http))
        return when (response) {
            is MessageResponse.Stream -> response.responses
            is MessageResponse.Task -> sequenceOf(StreamMessage.Task(response.task))
            is MessageResponse.Message -> sequenceOf(StreamMessage.Message(response.message))
        }
    }

    fun getTask(params: A2ATask.Get.Request.Params) = tasks.get(params.id, params.tenant)

    fun cancelTask(params: A2ATask.Cancel.Request.Params) = tasks.cancel(params.id, params.tenant)

    fun listTasks(params: A2ATask.ListTasks.Request.Params) =
        tasks.list(params.contextId, params.status, params.pageSize, params.pageToken, params.tenant)

    fun setPushConfig(params: A2APushNotificationConfig.Set.Request.Params): TaskPushNotificationConfig {
        val configId = PushNotificationConfigId.of(UUID.randomUUID().toString())
        val taskConfig = TaskPushNotificationConfig(
            id = configId,
            taskId = params.taskId,
            pushNotificationConfig = params.pushNotificationConfig,
            tenant = params.tenant
        )
        pushNotifications.store(taskConfig)
        return taskConfig
    }

    fun getPushConfig(params: A2APushNotificationConfig.Get.Request.Params) =
        pushNotifications.get(params.id, params.tenant)

    fun listPushConfigs(params: A2APushNotificationConfig.List.Request.Params) =
        pushNotifications.list(params.taskId, params.tenant)

    fun deletePushConfig(params: A2APushNotificationConfig.Delete.Request.Params): PushNotificationConfigId? {
        val existing = pushNotifications.get(params.id, params.tenant) ?: return null
        pushNotifications.delete(params.id, params.tenant)
        return existing.id
    }
}
