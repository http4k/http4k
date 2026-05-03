/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.protocol

import org.http4k.ai.a2a.MessageHandler
import org.http4k.ai.a2a.model.MessageRequest
import org.http4k.ai.a2a.model.MessageResponse
import org.http4k.ai.a2a.model.MessageStream
import org.http4k.ai.a2a.model.AgentCard
import org.http4k.ai.a2a.model.AgentCardProvider
import org.http4k.ai.a2a.model.Message
import org.http4k.ai.a2a.model.PushNotificationConfigId
import org.http4k.ai.a2a.model.StreamItem
import org.http4k.ai.a2a.model.Task
import org.http4k.ai.a2a.model.TaskPushNotificationConfig
import org.http4k.ai.a2a.protocol.messages.A2AMessage
import org.http4k.ai.a2a.protocol.messages.A2APushNotificationConfig
import org.http4k.ai.a2a.protocol.messages.A2ATask
import org.http4k.ai.a2a.server.storage.PushNotificationConfigStorage
import org.http4k.ai.a2a.server.storage.TaskStorage
import org.http4k.ai.a2a.util.A2AJson
import org.http4k.core.Request
import org.http4k.sse.SseMessage
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.UUID
import kotlin.concurrent.thread

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
        when (val response = handler(MessageRequest(params.message, params.configuration, params.metadata, http))) {
            is MessageStream -> when (val last = response.last()) {
                is Task -> last
                is Message -> last
                else -> error("Stream ended without task or message")
            }
            is Task, is Message -> response
        }

    fun stream(params: A2AMessage.Stream.Request.Params, http: Request): Sequence<StreamItem> =
        when (val response = handler(MessageRequest(params.message, params.configuration, params.metadata, http))) {
            is MessageStream -> response
            is Task -> sequenceOf(response)
            is Message -> sequenceOf(response)
        }

    fun getTask(params: A2ATask.Get.Request.Params) = tasks.get(params.id, params.historyLength, params.tenant)

    fun cancelTask(params: A2ATask.Cancel.Request.Params) = tasks.cancel(params.id, params.tenant)

    fun listTasks(params: A2ATask.ListTasks.Request.Params) =
        tasks.list(params.contextId, params.status, params.pageSize, params.pageToken, params.historyLength, params.includeArtifacts, params.tenant)

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

internal fun Sequence<StreamItem>.toSseStream(): InputStream {
    val pipedIn = PipedInputStream()
    val pipedOut = PipedOutputStream(pipedIn)

    thread(isDaemon = true) {
        pipedOut.use { out ->
            for (item in this) {
                out.write(SseMessage.Data(A2AJson.asJsonString(item, StreamItem::class)).toMessage().toByteArray())
                out.flush()
            }
        }
    }

    return pipedIn
}
