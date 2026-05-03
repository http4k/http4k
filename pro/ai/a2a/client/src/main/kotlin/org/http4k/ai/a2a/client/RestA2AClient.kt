/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.client

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.ai.a2a.A2AError
import org.http4k.ai.a2a.A2AResult
import org.http4k.ai.a2a.model.MessageResponse
import org.http4k.ai.a2a.model.AgentCard
import org.http4k.ai.a2a.model.ContextId
import org.http4k.ai.a2a.model.Message
import org.http4k.ai.a2a.model.PageToken
import org.http4k.ai.a2a.model.ResponseStream
import org.http4k.ai.a2a.protocol.ProtocolVersion
import org.http4k.ai.a2a.protocol.ProtocolVersion.Companion.LATEST_VERSION
import org.http4k.ai.a2a.model.PushNotificationConfig
import org.http4k.ai.a2a.model.PushNotificationConfigId
import org.http4k.ai.a2a.model.Task
import org.http4k.ai.a2a.model.TaskId
import org.http4k.ai.a2a.model.TaskPage
import org.http4k.ai.a2a.model.TaskPushNotificationConfig
import org.http4k.ai.a2a.model.StreamItem
import org.http4k.ai.a2a.model.TaskState
import org.http4k.ai.a2a.model.Tenant
import org.http4k.ai.a2a.protocol.messages.A2AMessage
import org.http4k.ai.a2a.protocol.messages.TaskConfiguration
import org.http4k.ai.a2a.util.A2AJson
import org.http4k.ai.a2a.util.A2AJson.auto
import org.http4k.ai.a2a.util.A2AJson.json
import org.http4k.client.JavaHttpClient
import org.http4k.core.Body
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ClientFilters
import org.http4k.format.MoshiObject
import org.http4k.lens.Query
import org.http4k.lens.boolean
import org.http4k.lens.enum
import org.http4k.lens.int
import org.http4k.lens.string
import org.http4k.lens.value
import org.http4k.sse.SseMessage
import org.http4k.sse.chunkedSseSequence

private val agentCardLens = Body.auto<AgentCard>().toLens()
private val sendMessageLens = Body.auto<A2AMessage.Send.Request.Params>().toLens()
private val taskLens = Body.auto<Task>().toLens()
private val taskPageLens = Body.auto<TaskPage>().toLens()
private val pushConfigLens = Body.auto<TaskPushNotificationConfig>().toLens()
private val pushConfigListLens = Body.auto<List<TaskPushNotificationConfig>>().toLens()
private val pushConfigInputLens = Body.auto<PushNotificationConfig>().toLens()
private val contextIdQuery = Query.value(ContextId).optional("contextId")
private val statusQuery = Query.enum<TaskState>().optional("status")
private val pageSizeQuery = Query.int().optional("pageSize")
private val pageTokenQuery = Query.value(PageToken).optional("pageToken")
private val historyLengthQuery = Query.int().optional("historyLength")
private val includeArtifactsQuery = Query.boolean().optional("includeArtifacts")

class RestA2AClient(
    baseUri: Uri,
    http: HttpHandler = JavaHttpClient(),
    tenant: Tenant? = null,
    protocolVersion: ProtocolVersion = LATEST_VERSION
) : A2AClient {

    private val client = ClientFilters.SetBaseUriFrom(baseUri)
        .then(Filter { next -> { next(it.header("A2A-Version", protocolVersion.value)) } })
        .then(http)
    private val prefix = tenant?.let { "/${it.value}" } ?: ""
    private val httpTasks: A2AClient.Tasks = RestTasks()
    private val httpPushConfigs: A2AClient.PushNotificationConfigs = RestPushNotificationConfigs()

    override fun agentCard(): A2AResult<AgentCard> {
        val response = client(Request(GET, "/.well-known/agent-card.json"))
        return when {
            response.status.successful -> Success(agentCardLens(response))
            else -> Failure(A2AError.Http(response))
        }
    }

    override fun extendedAgentCard(): A2AResult<AgentCard> {
        val response = client(Request(GET, "$prefix/extendedAgentCard"))
        return when {
            response.status.successful -> Success(agentCardLens(response))
            else -> Failure(A2AError.Http(response))
        }
    }

    override fun message(message: Message, configuration: TaskConfiguration?, metadata: Map<String, Any>?): A2AResult<MessageResponse> {
        val response = client(
            Request(POST, "$prefix/message:send").with(
                sendMessageLens of A2AMessage.Send.Request.Params(
                    message,
                    configuration,
                    metadata
                )
            )
        )
        return when {
            response.status.successful -> {
                val fields = A2AJson.fields(A2AJson.parse(response.bodyString()) as MoshiObject).toMap()
                Success(
                    when {
                        fields.containsKey("status") -> response.json<Task>()

                        else -> A2AJson.asA<Message>(response.bodyString())
                    }
                )
            }

            else -> Failure(A2AError.Http(response))
        }
    }

    override fun messageStream(message: Message, configuration: TaskConfiguration?, metadata: Map<String, Any>?): A2AResult<MessageResponse> {
        val response = client(
            Request(POST, "$prefix/message:stream").with(
                sendMessageLens of A2AMessage.Send.Request.Params(
                    message,
                    configuration,
                    metadata
                )
            )
        )
        return when {
            response.status.successful -> Success(
                ResponseStream(
                    response.body.stream.chunkedSseSequence()
                        .filterIsInstance<SseMessage.Data>()
                        .map { A2AJson.asA<StreamItem>(it.data) }
                )
            )

            else -> Failure(A2AError.Http(response))
        }
    }

    override fun tasks() = httpTasks

    override fun pushNotificationConfigs() = httpPushConfigs

    private inner class RestTasks : A2AClient.Tasks {
        override fun get(taskId: TaskId, historyLength: Int?): A2AResult<Task> {
            val response = client(
                Request(GET, "$prefix/tasks/${taskId.value}")
                    .with(historyLengthQuery of historyLength)
            )
            return when {
                response.status.successful -> Success(taskLens(response))
                else -> Failure(A2AError.Http(response))
            }
        }

        override fun subscribe(taskId: TaskId): A2AResult<MessageResponse> {
            val response = client(Request(GET, "$prefix/tasks/${taskId.value}:subscribe"))
            return when {
                response.status.successful -> Success(
                    ResponseStream(
                        response.body.stream.chunkedSseSequence()
                            .filterIsInstance<SseMessage.Data>()
                            .map { A2AJson.asA<StreamItem>(it.data) }
                    )
                )
                else -> Failure(A2AError.Http(response))
            }
        }

        override fun cancel(taskId: TaskId): A2AResult<Task> {
            val response = client(Request(POST, "$prefix/tasks/${taskId.value}:cancel"))
            return when {
                response.status.successful -> Success(taskLens(response))
                else -> Failure(A2AError.Http(response))
            }
        }

        override fun list(contextId: ContextId?, status: TaskState?, pageSize: Int?, pageToken: PageToken?, historyLength: Int?, includeArtifacts: Boolean?): A2AResult<TaskPage> {
            val response = client(
                Request(GET, "$prefix/tasks")
                    .with(contextIdQuery of contextId)
                    .with(statusQuery of status)
                    .with(pageSizeQuery of pageSize)
                    .with(pageTokenQuery of pageToken)
                    .with(historyLengthQuery of historyLength)
                    .with(includeArtifactsQuery of includeArtifacts)
            )
            return when {
                response.status.successful -> Success(taskPageLens(response))
                else -> Failure(A2AError.Http(response))
            }
        }
    }

    private inner class RestPushNotificationConfigs : A2AClient.PushNotificationConfigs {
        override fun set(taskId: TaskId, config: PushNotificationConfig): A2AResult<TaskPushNotificationConfig> {
            val response = client(
                Request(POST, "$prefix/tasks/${taskId.value}/pushNotificationConfigs")
                    .with(pushConfigInputLens of config)
            )
            return when {
                response.status.successful -> Success(pushConfigLens(response))
                else -> Failure(A2AError.Http(response))
            }
        }

        override fun get(taskId: TaskId, id: PushNotificationConfigId): A2AResult<TaskPushNotificationConfig> {
            val response = client(Request(GET, "$prefix/tasks/${taskId.value}/pushNotificationConfigs/${id.value}"))
            return when {
                response.status.successful -> Success(pushConfigLens(response))
                else -> Failure(A2AError.Http(response))
            }
        }

        override fun list(taskId: TaskId, pageSize: Int?, pageToken: PageToken?): A2AResult<List<TaskPushNotificationConfig>> {
            val response = client(
                Request(GET, "$prefix/tasks/${taskId.value}/pushNotificationConfigs")
                    .with(pageSizeQuery of pageSize)
                    .with(pageTokenQuery of pageToken)
            )
            return when {
                response.status.successful -> Success(pushConfigListLens(response))
                else -> Failure(A2AError.Http(response))
            }
        }

        override fun delete(taskId: TaskId, id: PushNotificationConfigId): A2AResult<PushNotificationConfigId> {
            val response = client(Request(DELETE, "$prefix/tasks/${taskId.value}/pushNotificationConfigs/${id.value}"))
            return when {
                response.status.successful -> Success(id)
                else -> Failure(A2AError.Http(response))
            }
        }
    }

    override fun close() {}
}
