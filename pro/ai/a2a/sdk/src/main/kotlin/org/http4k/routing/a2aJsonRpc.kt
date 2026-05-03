/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.routing

import org.http4k.ai.a2a.MessageHandler
import org.http4k.ai.a2a.model.AgentCard
import org.http4k.ai.a2a.model.AgentCardProvider
import org.http4k.ai.a2a.model.Message
import org.http4k.ai.a2a.model.MessageResponse
import org.http4k.ai.a2a.model.ResponseStream
import org.http4k.ai.a2a.model.Task
import org.http4k.ai.a2a.protocol.messages.A2AAgentCard
import org.http4k.ai.a2a.protocol.messages.A2AErrors
import org.http4k.ai.a2a.protocol.messages.A2AJsonRpcErrorResponse
import org.http4k.ai.a2a.protocol.messages.A2AJsonRpcRequest
import org.http4k.ai.a2a.protocol.messages.A2AJsonRpcResponse
import org.http4k.ai.a2a.protocol.messages.A2AMessage
import org.http4k.ai.a2a.protocol.messages.A2APushNotificationConfig
import org.http4k.ai.a2a.protocol.messages.A2ATask
import org.http4k.ai.a2a.server.A2AProtocolNegotiation
import org.http4k.ai.a2a.server.storage.PushNotificationConfigStorage
import org.http4k.ai.a2a.server.storage.TaskStorage
import org.http4k.ai.a2a.util.A2AJson.json
import org.http4k.core.ContentType
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.ServerFilters.CatchAll
import org.http4k.filter.ServerFilters.CatchLensFailure
import org.http4k.lens.contentType
import org.http4k.protocol.A2A
import org.http4k.protocol.toSseStream

/**
 * Create an A2A server using the JSON-RPC protocol binding.
 */
fun a2aJsonRpc(
    agentCard: AgentCard,
    messageHandler: MessageHandler,
    tasks: TaskStorage = TaskStorage.InMemory(),
    pushNotifications: PushNotificationConfigStorage = PushNotificationConfigStorage.InMemory(),
    rpcPath: String = "/",
) = a2aJsonRpc(A2A(agentCard, tasks, pushNotifications, messageHandler), rpcPath)

/**
 * Create an A2A server using the JSON-RPC protocol binding.
 */
fun a2aJsonRpc(
    cards: AgentCardProvider,
    messageHandler: MessageHandler,
    tasks: TaskStorage = TaskStorage.InMemory(),
    pushNotifications: PushNotificationConfigStorage = PushNotificationConfigStorage.InMemory(),
    rpcPath: String = "/",
) = a2aJsonRpc(A2A(cards, tasks, pushNotifications, messageHandler), rpcPath)

/**
 * Create an A2A server using the JSON-RPC protocol binding.
 */
fun a2aJsonRpc(a2a: A2A, rpcPath: String = "/") = CatchAll()
    .then(CatchLensFailure())
    .then(A2AProtocolNegotiation(a2a.cards.standard().capabilities))
    .then(
        routes(
            "/.well-known/agent-card.json" bind GET to { Response(OK).json(a2a.cards.standard()) },
            rpcPath bind POST to { httpReq ->
                val message = runCatching { httpReq.json<A2AJsonRpcRequest>() }
                    .getOrElse { return@to Response(BAD_REQUEST) }

                a2a.dispatchJsonRpc(message, httpReq)
            }
        )
    )

private fun A2A.dispatchJsonRpc(
    message: A2AJsonRpcRequest,
    httpReq: org.http4k.core.Request,
): Response {
    val capabilities = cards.standard().capabilities
    return when (message) {
        is A2AAgentCard.GetExtended.Request ->
            when (capabilities.extendedAgentCard) {
                true -> cards.extended()
                    .let { Response(OK).json(A2AAgentCard.GetExtended.Response(it, message.id)) }

                else -> Response(OK).json(A2AJsonRpcErrorResponse(message.id, A2AErrors.UnsupportedOperation))
            }

        is A2AMessage.Send.Request -> Response(OK).json(send(message.params, httpReq).toSendResponse(message.id))

        is A2AMessage.Stream.Request ->
            when (capabilities.streaming) {
                true -> Response(OK)
                    .contentType(ContentType.TEXT_EVENT_STREAM)
                    .body(stream(message.params, httpReq).toSseStream())

                else -> Response(OK).json(A2AJsonRpcErrorResponse(message.id, A2AErrors.UnsupportedOperation))
            }

        is A2ATask.Get.Request ->
            getTask(message.params)
                ?.let { Response(OK).json(A2ATask.Get.Response(A2ATask.Get.Response.Result(it), message.id)) }
                ?: Response(OK).json(A2AJsonRpcErrorResponse(message.id, A2AErrors.TaskNotFound))

        is A2ATask.Cancel.Request ->
            cancelTask(message.params)
                ?.let { Response(OK).json(A2ATask.Cancel.Response(A2ATask.Cancel.Response.Result(it), message.id)) }
                ?: Response(OK).json(A2AJsonRpcErrorResponse(message.id, A2AErrors.TaskNotFound))

        is A2ATask.ListTasks.Request -> {
            val page = listTasks(message.params)
            Response(OK).json(A2ATask.ListTasks.Response(A2ATask.ListTasks.Response.Result(
                page.tasks,
                page.totalSize,
                page.nextPageToken,
                message.params.pageSize
            ), message.id))
        }

        is A2ATask.Resubscribe.Request ->
            when (capabilities.streaming) {
                true -> Response(OK).json(
                    A2AJsonRpcErrorResponse(
                        message.id,
                        A2AErrors.UnsupportedOperation
                    )
                )

                else -> Response(OK).json(A2AJsonRpcErrorResponse(message.id, A2AErrors.UnsupportedOperation))
            }

        is A2APushNotificationConfig.Set.Request ->
            when (capabilities.pushNotifications) {
                true -> {
                    val config = setPushConfig(message.params)
                    Response(OK).json(
                        A2APushNotificationConfig.Set.Response(
                            A2APushNotificationConfig.Set.Response.Result(
                                config.id,
                                config.taskId,
                                config.pushNotificationConfig
                            ), message.id
                        )
                    )
                }

                else -> Response(OK).json(A2AJsonRpcErrorResponse(message.id, A2AErrors.PushNotificationNotSupported))
            }

        is A2APushNotificationConfig.Get.Request ->
            when (capabilities.pushNotifications) {
                true -> getPushConfig(message.params)
                    ?.let {
                        Response(OK).json(
                            A2APushNotificationConfig.Get.Response(
                                A2APushNotificationConfig.Get.Response.Result(
                                    it.id,
                                    it.taskId,
                                    it.pushNotificationConfig
                                ), message.id
                            )
                        )
                    }
                    ?: Response(OK).json(A2AJsonRpcErrorResponse(message.id, A2AErrors.TaskNotFound))

                else -> Response(OK).json(A2AJsonRpcErrorResponse(message.id, A2AErrors.PushNotificationNotSupported))
            }

        is A2APushNotificationConfig.List.Request ->
            when (capabilities.pushNotifications) {
                true -> {
                    val page = listPushConfigs(message.params)
                    Response(OK).json(
                        A2APushNotificationConfig.List.Response(
                            A2APushNotificationConfig.List.Response.Result(
                                page.configs,
                                page.nextPageToken
                            ), message.id
                        )
                    )
                }

                else -> Response(OK).json(A2AJsonRpcErrorResponse(message.id, A2AErrors.PushNotificationNotSupported))
            }

        is A2APushNotificationConfig.Delete.Request ->
            when (capabilities.pushNotifications) {
                true -> deletePushConfig(message.params)
                    ?.let {
                        Response(OK).json(
                            A2APushNotificationConfig.Delete.Response(
                                A2APushNotificationConfig.Delete.Response.Result(
                                    it
                                ), message.id
                            )
                        )
                    }
                    ?: Response(OK).json(A2AJsonRpcErrorResponse(message.id, A2AErrors.TaskNotFound))

                else -> Response(OK).json(A2AJsonRpcErrorResponse(message.id, A2AErrors.PushNotificationNotSupported))
            }
    }
}

private fun MessageResponse.toSendResponse(id: Any?): A2AJsonRpcResponse =
    when (this) {
        is Task -> A2AMessage.Send.Response.Task(this, id)
        is Message -> A2AMessage.Send.Response.Message(this, id)
        is ResponseStream -> error("unreachable")
    }
