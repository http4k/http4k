/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.client

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.map
import org.http4k.ai.a2a.A2AError
import org.http4k.ai.a2a.A2AResult
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
import org.http4k.ai.a2a.protocol.messages.A2AAgentCard
import org.http4k.ai.a2a.protocol.messages.A2AJsonRpcRequest
import org.http4k.ai.a2a.protocol.messages.A2AMessage
import org.http4k.ai.a2a.protocol.messages.A2APushNotificationConfig
import org.http4k.ai.a2a.protocol.messages.A2ATask
import org.http4k.ai.a2a.protocol.messages.TaskConfiguration
import org.http4k.ai.a2a.util.A2AJson
import org.http4k.ai.a2a.util.A2AJson.auto
import org.http4k.ai.a2a.util.A2ANodeType
import org.http4k.client.JavaHttpClient
import org.http4k.core.Accept
import org.http4k.core.Body
import org.http4k.core.BodyMode.Stream
import org.http4k.core.ContentType
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.QualifiedContent
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ClientFilters
import org.http4k.format.MoshiNode
import org.http4k.format.MoshiObject
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.lens.Header
import org.http4k.sse.SseMessage
import org.http4k.sse.chunkedSseSequence
import java.util.concurrent.atomic.AtomicLong

private val jsonRpcRequestLens = Body.auto<A2ANodeType>().toLens()

class HttpA2AClient(baseUri: Uri, http: HttpHandler = JavaHttpClient(responseBodyMode = Stream)) : A2AClient {

    private val client = ClientFilters.SetHostFrom(baseUri).then(http)
    private val requestId = AtomicLong(0)
    private val httpTasks: A2AClient.Tasks = ClientTasks()
    private val httpPushNotificationConfigs: A2AClient.PushNotificationConfigs = ClientPushNotificationConfigs()

    private fun nextId() = requestId.incrementAndGet()

    override fun agentCard(): A2AResult<AgentCard> {
        val response = client(Request(GET, "/.well-known/agent-card.json"))
        return when {
            response.status.successful -> Success(A2AJson.asA(response.bodyString()))
            else -> Failure(A2AError.Http(response))
        }
    }

    override fun extendedAgentCard(): A2AResult<AgentCard> =
        sendRpc<AgentCard>(A2AAgentCard.GetExtended.Request(nextId()))

    override fun message(message: Message, configuration: TaskConfiguration?): A2AResult<A2AMessage.Send.Response> =
        sendRpc<MoshiNode>(A2AMessage.Send.Request(A2AMessage.Send.Request.Params(message, configuration), nextId()))
            .map { parseSendResponse(it as MoshiObject) }

    override fun messageStream(message: Message, configuration: TaskConfiguration?): A2AResult<Sequence<A2AMessage.Send.Response>> {
        val request = A2AMessage.Stream.Request(A2AMessage.Stream.Request.Params(message, configuration), nextId())

        val response = client(
            Request(POST, "/")
                .with(jsonRpcRequestLens of A2AJson.asJsonObject(request))
                .with(Header.ACCEPT of Accept(listOf(QualifiedContent(ContentType.TEXT_EVENT_STREAM))))
        )

        return when {
            response.status.successful ->
                Success(
                    response.body.stream.chunkedSseSequence()
                        .filterIsInstance<SseMessage.Data>()
                        .map { parseStreamChunk(it.data) })

            else -> Failure(A2AError.Http(response))
        }
    }

    override fun tasks() = httpTasks

    override fun pushNotificationConfigs() = httpPushNotificationConfigs

    private fun parseSendResponse(resultNode: MoshiObject): A2AMessage.Send.Response = when {
        resultNode["status"] != null -> A2AMessage.Send.Response.Task(A2AJson.asA(resultNode, Task::class), null)
        else -> A2AMessage.Send.Response.Message(A2AJson.asA(resultNode, Message::class), null)
    }

    private fun parseStreamChunk(json: String): A2AMessage.Send.Response {
        val fields = A2AJson.fields(A2AJson.parse(json) as MoshiObject).toMap()
        return parseSendResponse(fields["result"] as MoshiObject)
    }

    private inner class ClientTasks : A2AClient.Tasks {
        override fun get(taskId: TaskId) =
            sendRpc<A2ATask.Get.Response.Result>(A2ATask.Get.Request(A2ATask.Get.Request.Params(taskId), nextId()))
                .map { it.task }

        override fun cancel(taskId: TaskId) =
            sendRpc<A2ATask.Cancel.Response.Result>(A2ATask.Cancel.Request(A2ATask.Cancel.Request.Params(taskId), nextId()))
                .map { it.task }

        override fun list(contextId: ContextId?, status: TaskState?, pageSize: Int?, pageToken: String?) =
            sendRpc<A2ATask.List.Response.Result>(A2ATask.List.Request(A2ATask.List.Request.Params(contextId, status, pageSize, pageToken), nextId()))
                .map { TaskPage(it.tasks, it.nextPageToken, it.totalSize) }
    }

    private inner class ClientPushNotificationConfigs : A2AClient.PushNotificationConfigs {
        override fun set(taskId: TaskId, config: PushNotificationConfig) =
            sendRpc<A2APushNotificationConfig.Set.Response.Result>(A2APushNotificationConfig.Set.Request(A2APushNotificationConfig.Set.Request.Params(taskId, config), nextId()))
                .map { TaskPushNotificationConfig(it.id, it.taskId, it.pushNotificationConfig) }

        override fun get(id: PushNotificationConfigId) =
            sendRpc<A2APushNotificationConfig.Get.Response.Result>(A2APushNotificationConfig.Get.Request(A2APushNotificationConfig.Get.Request.Params(id), nextId()))
                .map { TaskPushNotificationConfig(it.id, it.taskId, it.pushNotificationConfig) }

        override fun list(taskId: TaskId) =
            sendRpc<A2APushNotificationConfig.List.Response.Result>(A2APushNotificationConfig.List.Request(A2APushNotificationConfig.List.Request.Params(taskId), nextId()))
                .map { it.configs }

        override fun delete(id: PushNotificationConfigId) =
            sendRpc<A2APushNotificationConfig.Delete.Response.Result>(A2APushNotificationConfig.Delete.Request(A2APushNotificationConfig.Delete.Request.Params(id), nextId()))
                .map { it.id }
    }

    private inline fun <reified T : Any> sendRpc(request: A2AJsonRpcRequest): A2AResult<T> {
        val response = client(Request(POST, "/").with(jsonRpcRequestLens of A2AJson.asJsonObject(request)))
        val fields = A2AJson.fields(A2AJson.parse(response.bodyString()) as MoshiObject).toMap()

        return when {
            fields.containsKey("error") -> Failure(A2AError.Protocol(parseErrorMessage(fields["error"] as MoshiObject)))
            else -> Success(A2AJson.asA(fields["result"] as MoshiNode, T::class))
        }
    }

    private fun parseErrorMessage(errorNode: MoshiObject): ErrorMessage {
        val fields = A2AJson.fields(errorNode).toMap()
        val code = (fields["code"] as? Number)?.toInt() ?: -1
        return ErrorMessage(code, fields["message"]?.toString() ?: "Unknown error")
    }

    override fun close() {}
}
