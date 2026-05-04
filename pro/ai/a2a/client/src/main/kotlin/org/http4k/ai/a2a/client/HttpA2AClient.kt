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
import org.http4k.ai.a2a.model.AuthenticationInfo
import org.http4k.ai.a2a.model.ContextId
import org.http4k.ai.a2a.model.Message
import org.http4k.ai.a2a.model.MessageResponse
import org.http4k.ai.a2a.model.PageToken
import org.http4k.ai.a2a.model.PushNotificationConfigId
import org.http4k.ai.a2a.model.ResponseStream
import org.http4k.ai.a2a.model.StreamItem
import org.http4k.ai.a2a.model.Task
import org.http4k.ai.a2a.model.TaskId
import org.http4k.ai.a2a.model.TaskPage
import org.http4k.ai.a2a.model.TaskPushNotificationConfig
import org.http4k.ai.a2a.model.TaskState
import org.http4k.ai.a2a.model.Tenant
import org.http4k.ai.a2a.protocol.ProtocolVersion
import org.http4k.ai.a2a.protocol.ProtocolVersion.Companion.LATEST_VERSION
import org.http4k.ai.a2a.protocol.messages.A2AAgentCard
import org.http4k.ai.a2a.protocol.messages.A2AJsonRpcRequest
import org.http4k.ai.a2a.protocol.messages.A2AMessage
import org.http4k.ai.a2a.protocol.messages.A2APushNotificationConfig
import org.http4k.ai.a2a.protocol.messages.A2ATask
import org.http4k.ai.a2a.protocol.messages.SendMessageConfiguration
import org.http4k.ai.a2a.util.A2AJson
import org.http4k.ai.a2a.util.A2AJson.json
import org.http4k.client.JavaHttpClient
import org.http4k.core.Accept
import org.http4k.core.BodyMode.Stream
import org.http4k.core.ContentType
import org.http4k.core.Filter
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
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong

class HttpA2AClient(
    private val baseUri: Uri,
    http: HttpHandler = JavaHttpClient(responseBodyMode = Stream),
    private val tenant: Tenant? = null,
    protocolVersion: ProtocolVersion = LATEST_VERSION
) : A2AClient {

    private val client = ClientFilters.SetHostFrom(baseUri)
        .then(Filter { next -> { next(it.header("A2A-Version", protocolVersion.value)) } })
        .then(http)
    private val requestId = AtomicLong(0)
    private val httpTasks: A2AClient.Tasks = ClientTasks()
    private val httpPushNotificationConfigs: A2AClient.PushNotificationConfigs = ClientPushNotificationConfigs()

    private fun nextId() = requestId.incrementAndGet()

    override fun agentCard(): A2AResult<AgentCard> {
        val response = client(Request(GET, ".well-known/agent-card.json"))
        return when {
            response.status.successful -> Success(A2AJson.asA(response.bodyString()))
            else -> Failure(A2AError.Http(response))
        }
    }

    override fun extendedAgentCard(): A2AResult<AgentCard> =
        sendRpc<AgentCard>(A2AAgentCard.GetExtended.Request(params = A2AAgentCard.GetExtended.Request.Params(tenant = tenant), id = nextId()))

    override fun message(message: Message, configuration: SendMessageConfiguration?, metadata: Map<String, Any>?): A2AResult<MessageResponse> =
        sendRpc<MoshiNode>(A2AMessage.Send.Request(A2AMessage.Send.Request.Params(message, configuration, metadata, tenant = tenant), nextId()))
            .map { parseSendResponse(it as MoshiObject) }

    override fun messageStream(
        message: Message,
        configuration: SendMessageConfiguration?,
        metadata: Map<String, Any>?
    ) =
        A2AMessage.Stream.Request(
            A2AMessage.Stream.Request.Params(
                message,
                configuration,
                metadata,
                tenant = tenant
            ), nextId()
        ).sendAndStreamResponse()

    override fun tasks() = httpTasks

    override fun pushNotificationConfigs() = httpPushNotificationConfigs

    private fun parseSendResponse(resultNode: MoshiObject): MessageResponse = when {
        resultNode["status"] != null -> A2AJson.asA(resultNode, Task::class)
        else -> A2AJson.asA(resultNode, Message::class)
    }

    private inner class ClientTasks : A2AClient.Tasks {
        override fun get(taskId: TaskId, historyLength: Int?) =
            sendRpc<A2ATask.Get.Response.Result>(A2ATask.Get.Request(A2ATask.Get.Request.Params(taskId, historyLength, tenant = tenant), nextId()))
                .map { it.task }

        override fun subscribe(taskId: TaskId) = A2ATask.Resubscribe.Request(
            A2ATask.Resubscribe.Request.Params(
                taskId,
                tenant = tenant
            ), nextId()
        ).sendAndStreamResponse()

        override fun cancel(taskId: TaskId, metadata: Map<String, Any>?) =
            sendRpc<A2ATask.Cancel.Response.Result>(A2ATask.Cancel.Request(A2ATask.Cancel.Request.Params(taskId, metadata, tenant = tenant), nextId()))
                .map { it.task }

        override fun list(contextId: ContextId?, status: TaskState?, pageSize: Int?, pageToken: PageToken?, historyLength: Int?, statusTimestampAfter: Instant?, includeArtifacts: Boolean?) =
            sendRpc<A2ATask.ListTasks.Response.Result>(A2ATask.ListTasks.Request(A2ATask.ListTasks.Request.Params(contextId, status, pageSize, pageToken, historyLength, statusTimestampAfter, includeArtifacts, tenant = tenant), nextId()))
                .map { TaskPage(it.tasks, it.nextPageToken, it.pageSize, it.totalSize) }
    }

    private inner class ClientPushNotificationConfigs : A2AClient.PushNotificationConfigs {
        override fun set(taskId: TaskId, url: Uri, token: String?, authentication: AuthenticationInfo?) =
            sendRpc<TaskPushNotificationConfig>(A2APushNotificationConfig.Set.Request(A2APushNotificationConfig.Set.Request.Params(taskId, url, token, authentication, tenant), nextId()))

        override fun get(taskId: TaskId, id: PushNotificationConfigId) =
            sendRpc<TaskPushNotificationConfig>(A2APushNotificationConfig.Get.Request(A2APushNotificationConfig.Get.Request.Params(taskId, id, tenant = tenant), nextId()))

        override fun list(taskId: TaskId, pageSize: Int?, pageToken: PageToken?) =
            sendRpc<A2APushNotificationConfig.List.Response.Result>(A2APushNotificationConfig.List.Request(A2APushNotificationConfig.List.Request.Params(taskId, pageSize, pageToken, tenant = tenant), nextId()))
                .map { it.configs }

        override fun delete(taskId: TaskId, id: PushNotificationConfigId) =
            sendRpc<A2APushNotificationConfig.Delete.Response.Result>(A2APushNotificationConfig.Delete.Request(A2APushNotificationConfig.Delete.Request.Params(taskId, id, tenant = tenant), nextId()))
                .map { it.id }
    }

    private inline fun <reified T : Any> sendRpc(request: A2AJsonRpcRequest): A2AResult<T> {
        val response = client(Request(POST, baseUri.path).json(request))
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

    private fun A2AJsonRpcRequest.sendAndStreamResponse(): A2AResult<MessageResponse> {
        val response = client(
            Request(POST, baseUri.path)
                .json(this)
                .with(Header.ACCEPT of Accept(listOf(QualifiedContent(ContentType.TEXT_EVENT_STREAM))))
        )
        return when {
            response.status.successful ->
                Success(
                    ResponseStream(
                        response.body.stream.chunkedSseSequence()
                            .filterIsInstance<SseMessage.Data>()
                            .map { A2AJson.asA<StreamItem>(it.data) }
                    )
                )

            else -> Failure(A2AError.Http(response))
        }
    }

    override fun close() {}
}
