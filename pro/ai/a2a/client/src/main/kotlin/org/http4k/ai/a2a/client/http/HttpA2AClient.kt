/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.client.http

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.ai.a2a.A2AError
import org.http4k.ai.a2a.A2AResult
import org.http4k.ai.a2a.client.A2AClient
import org.http4k.ai.a2a.model.AgentCard
import org.http4k.ai.a2a.model.Message
import org.http4k.ai.a2a.model.PushNotificationConfigId
import org.http4k.ai.a2a.model.Task
import org.http4k.ai.a2a.model.TaskId
import org.http4k.ai.a2a.protocol.messages.A2AJsonRpcRequest
import org.http4k.ai.a2a.protocol.messages.A2AMessage
import org.http4k.ai.a2a.protocol.messages.A2APushNotificationConfig
import org.http4k.ai.a2a.protocol.messages.A2ATask
import org.http4k.ai.a2a.model.PushNotificationConfig
import org.http4k.ai.a2a.model.TaskPushNotificationConfig
import org.http4k.ai.a2a.util.A2AJson
import org.http4k.ai.a2a.util.A2AJson.auto
import org.http4k.ai.a2a.util.A2ANodeType
import org.http4k.client.JavaHttpClient
import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ClientFilters
import org.http4k.format.MoshiObject
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.jsonrpc.JsonRpcResult
import org.http4k.sse.SseMessage
import org.http4k.sse.chunkedSseSequence
import java.util.concurrent.atomic.AtomicLong

private val agentCardLens = Body.auto<AgentCard>().toLens()
private val jsonRpcRequestLens = Body.auto<A2ANodeType>().toLens()

fun A2AClient.Companion.Http(
    baseUri: Uri,
    http: HttpHandler = JavaHttpClient(),
    rpcPath: String = "/",
    agentCardPath: String = "/.well-known/agent.json"
): A2AClient = HttpA2AClient(baseUri, http, rpcPath, agentCardPath)

class HttpA2AClient(
    baseUri: Uri,
    http: HttpHandler,
    private val rpcPath: String = "/",
    private val agentCardPath: String = "/.well-known/agent.json"
) : A2AClient {

    private val client = ClientFilters.SetBaseUriFrom(baseUri).then(http)
    private val requestId = AtomicLong(0)

    private fun nextId(): Any = requestId.incrementAndGet()

    override fun agentCard(): A2AResult<AgentCard> {
        val response = client(Request(GET, agentCardPath))
        return Success(agentCardLens(response))
    }

    override fun message(message: Message): A2AResult<A2AMessage.Send.Response> {
        val request = A2AMessage.Send.Request(
            A2AMessage.Send.Request.Params(message),
            nextId()
        )

        return sendRpc(request) { resultNode ->
            val resultFields = A2AJson.fields(resultNode).toMap()
            if (resultFields.containsKey("task")) {
                A2AJson.asA<A2AMessage.Send.Response.Task>(A2AJson.asFormatString(resultNode))
            } else {
                A2AJson.asA<A2AMessage.Send.Response.Message>(A2AJson.asFormatString(resultNode))
            }
        }
    }

    override fun messageStream(message: Message): A2AResult<Sequence<A2AMessage.Send.Response>> {
        val request = A2AMessage.Stream.Request(
            A2AMessage.Stream.Request.Params(message),
            nextId()
        )

        val response = client(
            Request(POST, rpcPath)
                .with(jsonRpcRequestLens of A2AJson.asJsonObject(request))
                .header("Accept", "text/event-stream")
        )

        if (!response.status.successful) {
            return Failure(A2AError.Http(response))
        }

        val responseSequence = response.body.stream.chunkedSseSequence()
            .filterIsInstance<SseMessage.Data>()
            .map { sseMessage -> parseStreamResponse(sseMessage.data) }

        return Success(responseSequence)
    }

    private fun parseStreamResponse(json: String): A2AMessage.Send.Response {
        val jsonResult = JsonRpcResult(A2AJson, A2AJson.fields(A2AJson.parse(json) as MoshiObject).toMap())
        val resultNode = jsonResult.result as? MoshiObject
            ?: throw IllegalStateException("Invalid stream response")
        val resultFields = A2AJson.fields(resultNode).toMap()

        return if (resultFields.containsKey("task")) {
            A2AJson.asA<A2AMessage.Send.Response.Task>(A2AJson.asFormatString(resultNode))
        } else {
            A2AJson.asA<A2AMessage.Send.Response.Message>(A2AJson.asFormatString(resultNode))
        }
    }

    override fun tasks(): A2AClient.Tasks = HttpA2ATasks()

    override fun pushNotificationConfigs(): A2AClient.PushNotificationConfigs = HttpA2APushNotificationConfigs()

    private inner class HttpA2ATasks : A2AClient.Tasks {
        override fun get(taskId: TaskId): A2AResult<Task> =
            sendRpc(A2ATask.Get.Request(A2ATask.Get.Request.Params(taskId), nextId())) {
                A2AJson.asA<A2ATask.Get.Response.Result>(A2AJson.asFormatString(it)).task
            }

        override fun cancel(taskId: TaskId): A2AResult<Task> =
            sendRpc(A2ATask.Cancel.Request(A2ATask.Cancel.Request.Params(taskId), nextId())) {
                A2AJson.asA<A2ATask.Cancel.Response.Result>(A2AJson.asFormatString(it)).task
            }

        override fun list(params: A2ATask.List.Request.Params): A2AResult<A2ATask.List.Response.Result> =
            sendRpc(A2ATask.List.Request(params, nextId())) {
                A2AJson.asA<A2ATask.List.Response.Result>(A2AJson.asFormatString(it))
            }
    }

    private inner class HttpA2APushNotificationConfigs : A2AClient.PushNotificationConfigs {
        override fun set(taskId: TaskId, config: PushNotificationConfig): A2AResult<TaskPushNotificationConfig> =
            sendRpc(A2APushNotificationConfig.Set.Request(A2APushNotificationConfig.Set.Request.Params(taskId, config), nextId())) {
                val result = A2AJson.asA<A2APushNotificationConfig.Set.Response.Result>(A2AJson.asFormatString(it))
                TaskPushNotificationConfig(result.id, result.taskId, result.pushNotificationConfig)
            }

        override fun get(id: PushNotificationConfigId): A2AResult<TaskPushNotificationConfig> =
            sendRpc(A2APushNotificationConfig.Get.Request(A2APushNotificationConfig.Get.Request.Params(id), nextId())) {
                val result = A2AJson.asA<A2APushNotificationConfig.Get.Response.Result>(A2AJson.asFormatString(it))
                TaskPushNotificationConfig(result.id, result.taskId, result.pushNotificationConfig)
            }

        override fun list(taskId: TaskId): A2AResult<List<TaskPushNotificationConfig>> =
            sendRpc(A2APushNotificationConfig.List.Request(A2APushNotificationConfig.List.Request.Params(taskId), nextId())) {
                A2AJson.asA<A2APushNotificationConfig.List.Response.Result>(A2AJson.asFormatString(it)).configs
            }

        override fun delete(id: PushNotificationConfigId): A2AResult<PushNotificationConfigId> =
            sendRpc(A2APushNotificationConfig.Delete.Request(A2APushNotificationConfig.Delete.Request.Params(id), nextId())) {
                A2AJson.asA<A2APushNotificationConfig.Delete.Response.Result>(A2AJson.asFormatString(it)).id
            }
    }

    private fun <T> sendRpc(request: A2AJsonRpcRequest, parse: (MoshiObject) -> T): A2AResult<T> {
        val response = client(Request(POST, rpcPath).with(jsonRpcRequestLens of A2AJson.asJsonObject(request)))
        val jsonResult = JsonRpcResult(
            A2AJson,
            A2AJson.fields(A2AJson.parse(response.bodyString()) as MoshiObject).toMap()
        )

        return when {
            jsonResult.isError() -> Failure(A2AError.Protocol(parseErrorMessage(jsonResult.error!!)))
            else -> Success(parse(jsonResult.result as MoshiObject))
        }
    }

    private fun parseErrorMessage(errorNode: A2ANodeType): ErrorMessage {
        val fields = A2AJson.fields(errorNode as MoshiObject).toMap()
        val code = (fields["code"] as? Number)?.toInt() ?: -1
        val message = A2AJson.text(fields["message"] ?: A2AJson.string("Unknown error"))
        return ErrorMessage(code, message)
    }

    override fun close() {}
}
