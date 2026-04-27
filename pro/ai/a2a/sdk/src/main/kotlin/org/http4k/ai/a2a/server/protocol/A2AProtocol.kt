/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.server.protocol

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result4k
import dev.forkhandles.result4k.Success
import org.http4k.ai.a2a.MessageHandler
import org.http4k.ai.a2a.model.AgentCard
import org.http4k.ai.a2a.protocol.messages.A2AJsonRpcErrorResponse
import org.http4k.ai.a2a.protocol.messages.A2AJsonRpcRequest
import org.http4k.ai.a2a.protocol.messages.A2AMessage
import org.http4k.ai.a2a.protocol.messages.A2APushNotificationConfig
import org.http4k.ai.a2a.protocol.messages.A2ATask
import org.http4k.ai.a2a.server.capability.MessageCapability
import org.http4k.ai.a2a.server.capability.ServerCapability
import org.http4k.ai.a2a.server.capability.messages
import org.http4k.ai.a2a.server.capability.pushNotificationConfigs
import org.http4k.ai.a2a.server.capability.tasks
import org.http4k.ai.a2a.util.A2AJson
import org.http4k.ai.a2a.util.A2AJson.nullNode
import org.http4k.ai.a2a.util.A2ANodeType
import org.http4k.core.Request
import org.http4k.format.renderResult
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidParams

/**
 * Models the A2A protocol in terms of message handling.
 */
class A2AProtocol(
    val agentCard: AgentCard,
    private val messages: Messages,
    private val tasks: Tasks = tasks(),
    private val pushNotificationConfigs: PushNotificationConfigs = pushNotificationConfigs(),
    private val onError: (Throwable) -> Unit = { it.printStackTrace(System.err) }
) {
    constructor(
        agentCard: AgentCard,
        messageHandler: MessageHandler,
        tasks: Tasks = tasks(),
        pushNotificationConfigs: PushNotificationConfigs = pushNotificationConfigs(),
        onError: (Throwable) -> Unit = { it.printStackTrace(System.err) }
    ) : this(agentCard, messages(messageHandler), tasks, pushNotificationConfigs, onError)

    constructor(
        agentCard: AgentCard,
        vararg capabilities: ServerCapability,
        onError: (Throwable) -> Unit = { it.printStackTrace(System.err) }
    ) : this(
        agentCard,
        messages(
            capabilities.flatMap { it }.filterIsInstance<MessageCapability>()
                .firstOrNull()?.handler ?: error("No MessageCapability provided")
        ),
        onError = onError
    )

    operator fun invoke(httpReq: Request): Result4k<A2AProtocolResponse, A2ANodeType> {
        val body = httpReq.bodyString()
        val message = runCatching { A2AJson.asA<A2AJsonRpcRequest>(body) }
            .getOrElse { return Failure(nullNode()) }

        return dispatch(message, httpReq)
    }

    private fun dispatch(
        message: A2AJsonRpcRequest,
        httpReq: Request
    ): Result4k<A2AProtocolResponse, A2ANodeType> = when (message) {
        is A2AMessage.Send.Request -> respondWithResult(message.id) {
            messages.send(message.params, httpReq)
        }

        is A2AMessage.Stream.Request -> handleStream(message.id) {
            messages.stream(message.params, httpReq)
        }

        is A2ATask.Get.Request -> respond(message.id) {
            tasks.get(message.params)?.let { A2ATask.Get.Response(it, message.id) }
        }

        is A2ATask.Cancel.Request -> respond(message.id) {
            tasks.cancel(message.params)?.let { A2ATask.Cancel.Response(it, message.id) }
        }

        is A2ATask.List.Request -> respond(message.id) {
            A2ATask.List.Response(tasks.list(message.params), message.id)
        }

        is A2ATask.Resubscribe.Request -> respond(message.id) { null }

        is A2APushNotificationConfig.Set.Request -> respond(message.id) {
            A2APushNotificationConfig.Set.Response(pushNotificationConfigs.set(message.params), message.id)
        }

        is A2APushNotificationConfig.Get.Request -> respond(message.id) {
            pushNotificationConfigs.get(message.params)
                ?.let { A2APushNotificationConfig.Get.Response(it, message.id) }
        }

        is A2APushNotificationConfig.List.Request -> respond(message.id) {
            A2APushNotificationConfig.List.Response(pushNotificationConfigs.list(message.params), message.id)
        }

        is A2APushNotificationConfig.Delete.Request -> respond(message.id) {
            pushNotificationConfigs.delete(message.params)
                ?.let { A2APushNotificationConfig.Delete.Response(it, message.id) }
        }
    }

    private fun respondWithResult(
        id: Any?,
        handler: () -> Any?
    ): Result4k<A2AProtocolResponse, A2ANodeType> =
        runCatching {
            val response = handler()
            if (response != null) {
                single(A2AJson.renderResult(A2AJson.asJsonObject(response), A2AJson.asJsonObject(id ?: nullNode())))
            } else {
                single(A2AJson.asJsonObject(A2AJsonRpcErrorResponse(id, InvalidParams)))
            }
        }.getOrElse {
            onError(it)
            single(A2AJson.asJsonObject(A2AJsonRpcErrorResponse(id, InvalidParams)))
        }

    private fun handleStream(
        id: Any?,
        handler: () -> Sequence<A2AMessage.Send.Response>
    ): Result4k<A2AProtocolResponse, A2ANodeType> =
        runCatching {
            Success(A2AProtocolResponse.Stream(handler().map {
                A2AJson.renderResult(A2AJson.asJsonObject(it), A2AJson.asJsonObject(id ?: nullNode()))
            }))
        }.getOrElse {
            onError(it)
            single(A2AJson.asJsonObject(A2AJsonRpcErrorResponse(id, InvalidParams)))
        }

    private fun respond(
        id: Any?,
        handler: () -> Any?
    ): Result4k<A2AProtocolResponse, A2ANodeType> =
        runCatching {
            val response = handler()
            if (response != null) {
                single(A2AJson.asJsonObject(response))
            } else {
                single(A2AJson.asJsonObject(A2AJsonRpcErrorResponse(id, InvalidParams)))
            }
        }.getOrElse {
            onError(it)
            single(A2AJson.asJsonObject(A2AJsonRpcErrorResponse(id, InvalidParams)))
        }

    private fun single(response: A2ANodeType) = Success(A2AProtocolResponse.Single(response))
}
