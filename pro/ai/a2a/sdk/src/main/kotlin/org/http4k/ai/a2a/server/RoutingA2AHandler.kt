/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.server

import org.http4k.ai.a2a.A2AHandler
import org.http4k.ai.a2a.A2AResponse
import org.http4k.ai.a2a.MessageHandler
import org.http4k.ai.a2a.MessageRequest
import org.http4k.ai.a2a.MessageResponse
import org.http4k.ai.a2a.model.PushNotificationConfigId
import org.http4k.ai.a2a.model.TaskPushNotificationConfig
import org.http4k.ai.a2a.protocol.messages.A2AJsonRpcErrorResponse
import org.http4k.ai.a2a.protocol.messages.A2AJsonRpcResponse
import org.http4k.ai.a2a.protocol.messages.A2AMessage
import org.http4k.ai.a2a.protocol.messages.A2APushNotificationConfig
import org.http4k.ai.a2a.protocol.messages.A2ATask
import org.http4k.ai.a2a.server.storage.PushNotificationConfigStorage
import org.http4k.ai.a2a.server.storage.TaskStorage
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidParams
import java.util.UUID

fun RoutingA2AHandler(
    handler: MessageHandler,
    tasks: TaskStorage = TaskStorage.InMemory(),
    pushNotifications: PushNotificationConfigStorage = PushNotificationConfigStorage.InMemory()
): A2AHandler = { request ->
    when (val message = request.message) {
        is A2AMessage.Send.Request -> {
            val response = handler(MessageRequest(message.params.message, request.http))
            A2AResponse.Single(response.toSendResponse(message.id))
        }

        is A2AMessage.Stream.Request -> {
            val response = handler(MessageRequest(message.params.message, request.http))
            A2AResponse.Stream(response.toStreamResponses(message.id))
        }

        is A2ATask.Get.Request -> respondSingle(message.id) {
            tasks.get(message.params.id)?.let { A2ATask.Get.Response(A2ATask.Get.Response.Result(it), message.id) }
        }

        is A2ATask.Cancel.Request -> respondSingle(message.id) {
            tasks.cancel(message.params.id)?.let { A2ATask.Cancel.Response(A2ATask.Cancel.Response.Result(it), message.id) }
        }

        is A2ATask.List.Request -> respondSingle(message.id) {
            val page = tasks.list(message.params.contextId, message.params.status, message.params.pageSize, message.params.pageToken)
            A2ATask.List.Response(A2ATask.List.Response.Result(page.tasks, page.nextPageToken, message.params.pageSize, page.totalSize), message.id)
        }

        is A2ATask.Resubscribe.Request -> A2AResponse.Single(A2AJsonRpcErrorResponse(message.id, InvalidParams))

        is A2APushNotificationConfig.Set.Request -> respondSingle(message.id) {
            val configId = PushNotificationConfigId.of(UUID.randomUUID().toString())
            val taskConfig = TaskPushNotificationConfig(
                id = configId,
                taskId = message.params.taskId,
                pushNotificationConfig = message.params.pushNotificationConfig
            )
            pushNotifications.store(taskConfig)
            A2APushNotificationConfig.Set.Response(
                A2APushNotificationConfig.Set.Response.Result(configId, message.params.taskId, message.params.pushNotificationConfig),
                message.id
            )
        }

        is A2APushNotificationConfig.Get.Request -> respondSingle(message.id) {
            pushNotifications.get(message.params.id)?.let {
                A2APushNotificationConfig.Get.Response(
                    A2APushNotificationConfig.Get.Response.Result(it.id, it.taskId, it.pushNotificationConfig),
                    message.id
                )
            }
        }

        is A2APushNotificationConfig.List.Request -> respondSingle(message.id) {
            A2APushNotificationConfig.List.Response(
                A2APushNotificationConfig.List.Response.Result(pushNotifications.list(message.params.taskId)),
                message.id
            )
        }

        is A2APushNotificationConfig.Delete.Request -> respondSingle(message.id) {
            val existing = pushNotifications.get(message.params.id) ?: return@respondSingle null
            pushNotifications.delete(message.params.id)
            A2APushNotificationConfig.Delete.Response(
                A2APushNotificationConfig.Delete.Response.Result(existing.id),
                message.id
            )
        }
    }
}

private fun respondSingle(id: Any?, handler: () -> A2AJsonRpcResponse?): A2AResponse.Single =
    A2AResponse.Single(handler() ?: A2AJsonRpcErrorResponse(id, InvalidParams))

private fun MessageResponse.toSendResponse(id: Any?): A2AJsonRpcResponse = when (this) {
    is MessageResponse.Task -> A2AMessage.Send.Response.Task(tasks.first(), id)
    is MessageResponse.Message -> A2AMessage.Send.Response.Message(message, id)
}

private fun MessageResponse.toStreamResponses(id: Any?): Sequence<A2AJsonRpcResponse> = when (this) {
    is MessageResponse.Task -> tasks.map { A2AMessage.Send.Response.Task(it, id) }
    is MessageResponse.Message -> sequenceOf(A2AMessage.Send.Response.Message(message, id))
}
