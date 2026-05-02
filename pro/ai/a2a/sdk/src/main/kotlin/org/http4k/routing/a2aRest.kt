/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.routing

import org.http4k.ai.a2a.MessageHandler
import org.http4k.ai.a2a.model.toWire
import org.http4k.ai.a2a.protocol.messages.toWire
import org.http4k.ai.a2a.MessageResponse
import org.http4k.ai.a2a.MessageResponse.Message
import org.http4k.ai.a2a.MessageResponse.Stream
import org.http4k.ai.a2a.MessageResponse.Task
import org.http4k.ai.a2a.model.AgentCard
import org.http4k.ai.a2a.model.AgentCardProvider
import org.http4k.ai.a2a.model.ContextId
import org.http4k.ai.a2a.model.PushNotificationConfigId
import org.http4k.ai.a2a.model.TaskId
import org.http4k.ai.a2a.model.TaskState
import org.http4k.ai.a2a.model.Tenant
import org.http4k.ai.a2a.protocol.messages.A2AMessage
import org.http4k.ai.a2a.protocol.messages.A2APushNotificationConfig
import org.http4k.ai.a2a.protocol.messages.A2ATask
import org.http4k.ai.a2a.protocol.messages.A2ATaskPage
import org.http4k.ai.a2a.server.storage.PushNotificationConfigStorage
import org.http4k.ai.a2a.server.storage.TaskStorage
import org.http4k.ai.a2a.util.A2AJson
import org.http4k.ai.a2a.util.A2AJson.json
import org.http4k.core.ContentType
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.CREATED
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.NO_CONTENT
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.ServerFilters.CatchAll
import org.http4k.filter.ServerFilters.CatchLensFailure
import org.http4k.lens.Path
import org.http4k.lens.Query
import org.http4k.lens.contentType
import org.http4k.lens.enum
import org.http4k.lens.int
import org.http4k.lens.string
import org.http4k.lens.value
import org.http4k.protocol.A2A
import org.http4k.sse.SseMessage
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import kotlin.concurrent.thread


/**
 * Create an A2A server using the HTTP/REST protocol binding.
 */
fun a2aRest(
    agentCard: AgentCard,
    messageHandler: MessageHandler,
    tasks: TaskStorage = TaskStorage.InMemory(),
    pushNotifications: PushNotificationConfigStorage = PushNotificationConfigStorage.InMemory(),
    basePath: String = ""
) = a2aRest(A2A(agentCard, tasks, pushNotifications, messageHandler), basePath)

fun a2aRest(
    cards: AgentCardProvider,
    messageHandler: MessageHandler,
    tasks: TaskStorage = TaskStorage.InMemory(),
    pushNotifications: PushNotificationConfigStorage = PushNotificationConfigStorage.InMemory(),
    basePath: String = ""
) = a2aRest(A2A(cards, tasks, pushNotifications, messageHandler), basePath)

/**
 * Create an A2A server using the HTTP/REST protocol binding.
 */
fun a2aRest(
    protocol: A2A,
    basePath: String = ""
) = CatchAll()
        .then(CatchLensFailure())
        .then(
            routes(
                basePath bind routes(
                    a2aEndpoints(protocol),
                    "{tenant}" bind a2aEndpoints(protocol)
                )
            )
        )

private fun a2aEndpoints(protocol: A2A): RoutingHttpHandler = routes(
    "/.well-known/agent-card.json" bind GET to { Response(OK).json(protocol.cards.standard()) },

    "extendedAgentCard" bind GET to { protocol.cards.extended().let { Response(OK).json(it) } },

    "message:send" bind POST to { req ->
        val params = req.json<A2AMessage.Send.Request.Params>().withTenant(req)
        when (val response = protocol.send(params, req)) {
            is Task -> Response(OK).json(response.task.toWire())
            is Message -> Response(OK).json(response.message.toWire())
            is Stream -> when (val last = response.responses.last()) {
                is org.http4k.ai.a2a.model.StreamMessage.Task -> Response(OK).json(last.task.toWire())
                is org.http4k.ai.a2a.model.StreamMessage.Message -> Response(OK).json(last.message.toWire())
                else -> error("Stream ended without task or message")
            }
        }
    },

    "message:stream" bind POST to { req ->
        val params = A2AMessage.Stream.Request.Params(req.json<A2AMessage.Send.Request.Params>().message, tenant = req.tenant())
        val responses = protocol.stream(params, req)
        Response(OK)
            .contentType(ContentType.TEXT_EVENT_STREAM)
            .body(responses.toRestSseStream())
    },

    "tasks" bind routes(
        "{taskId}" bind GET to { req ->
            protocol.getTask(A2ATask.Get.Request.Params(taskIdPath(req), tenant = req.tenant()))
                ?.let { Response(OK).json(it.toWire()) }
                ?: Response(NOT_FOUND)
        },

        "{taskId}:cancel" bind POST to { req ->
            protocol.cancelTask(A2ATask.Cancel.Request.Params(taskIdPath(req), tenant = req.tenant()))
                ?.let { Response(OK).json(it.toWire()) }
                ?: Response(NOT_FOUND)
        },

        "{taskId}/pushNotificationConfigs" bind POST to { req ->
            Response(CREATED).json(
                protocol.setPushConfig(
                    A2APushNotificationConfig.Set.Request.Params(taskIdPath(req), req.json(), tenant = req.tenant())
                )
            )
        },

        "{taskId}/pushNotificationConfigs/{configId}" bind GET to { req ->
            protocol.getPushConfig(A2APushNotificationConfig.Get.Request.Params(taskIdPath(req), configIdPath(req), tenant = req.tenant()))
                ?.let { Response(OK).json(it) }
                ?: Response(NOT_FOUND)
        },

        "{taskId}/pushNotificationConfigs" bind GET to { req ->
            Response(OK).json(protocol.listPushConfigs(A2APushNotificationConfig.List.Request.Params(taskIdPath(req), tenant = req.tenant())))
        },

        "{taskId}/pushNotificationConfigs/{configId}" bind DELETE to { req ->
            if (protocol.deletePushConfig(A2APushNotificationConfig.Delete.Request.Params(taskIdPath(req), configIdPath(req), tenant = req.tenant())) != null) Response(NO_CONTENT)
            else Response(NOT_FOUND)
        },

        "" bind GET to { req ->
            val page = protocol.listTasks(
                A2ATask.ListTasks.Request.Params(
                    contextId = contextIdQuery(req),
                    status = statusQuery(req),
                    pageSize = pageSizeQuery(req),
                    pageToken = pageTokenQuery(req),
                    tenant = req.tenant()
                )
            )
            Response(OK).json(A2ATaskPage(page.tasks.map { it.toWire() }, page.nextPageToken, page.totalSize))
        }
    )
)

private fun A2AMessage.Send.Request.Params.withTenant(req: org.http4k.core.Request) =
    copy(tenant = req.tenant())

private fun org.http4k.core.Request.tenant(): Tenant? = path("tenant")?.let { Tenant.of(it) }

private val taskIdPath = Path.value(TaskId).of("taskId")
private val configIdPath = Path.value(PushNotificationConfigId).of("configId")
private val contextIdQuery = Query.value(ContextId).optional("contextId")
private val statusQuery = Query.enum<TaskState>().optional("status")
private val pageSizeQuery = Query.int().optional("pageSize")
private val pageTokenQuery = Query.string().optional("pageToken")

private fun Sequence<org.http4k.ai.a2a.model.StreamMessage>.toRestSseStream(): InputStream {
    val pipedIn = PipedInputStream()
    val pipedOut = PipedOutputStream(pipedIn)

    thread(isDaemon = true) {
        pipedOut.use { out ->
            for (msg in this) {
                out.write(SseMessage.Data(A2AJson.asFormatString(msg.toWire())).toMessage().toByteArray())
                out.flush()
            }
        }
    }

    return pipedIn
}
