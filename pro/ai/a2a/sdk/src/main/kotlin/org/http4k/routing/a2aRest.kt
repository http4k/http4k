/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.routing

import org.http4k.ai.a2a.MessageHandler
import org.http4k.ai.a2a.model.AgentCard
import org.http4k.ai.a2a.model.AgentCardProvider
import org.http4k.ai.a2a.model.ContextId
import org.http4k.ai.a2a.model.CreateTaskPushNotificationConfig
import org.http4k.ai.a2a.model.Message
import org.http4k.ai.a2a.model.PageToken
import org.http4k.ai.a2a.model.PushNotificationConfigId
import org.http4k.ai.a2a.model.ResponseStream
import org.http4k.ai.a2a.model.Task
import org.http4k.ai.a2a.model.TaskId
import org.http4k.ai.a2a.model.TaskState
import org.http4k.ai.a2a.model.Tenant
import org.http4k.ai.a2a.protocol.messages.A2AMessage
import org.http4k.ai.a2a.protocol.messages.A2APushNotificationConfig
import org.http4k.ai.a2a.protocol.messages.A2ATask
import org.http4k.ai.a2a.server.A2AProtocolNegotiation
import org.http4k.ai.a2a.server.TaskSubscriptions
import org.http4k.ai.a2a.server.storage.PushNotificationConfigStorage
import org.http4k.ai.a2a.server.storage.TaskStorage
import org.http4k.ai.a2a.util.A2AJson.json
import org.http4k.core.ContentType
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.PolyHandler
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.CREATED
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.NO_CONTENT
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.ServerFilters.CatchAll
import org.http4k.filter.ServerFilters.CatchLensFailure
import org.http4k.lens.Path
import org.http4k.lens.Query
import org.http4k.lens.boolean
import org.http4k.lens.contentType
import org.http4k.lens.enum
import org.http4k.lens.instant
import org.http4k.lens.int
import org.http4k.lens.value
import org.http4k.protocol.A2A
import org.http4k.protocol.toSseStream
import org.http4k.sse.SseResponse


/**
 * Create an A2A server using the HTTP/REST protocol binding.
 */
fun a2aRest(
    agentCard: AgentCard,
    tasks: TaskStorage = TaskStorage.InMemory(),
    pushNotifications: PushNotificationConfigStorage = PushNotificationConfigStorage.InMemory(),
    subscriptions: TaskSubscriptions = TaskSubscriptions.InMemory(),
    basePath: String = "",
    messageHandler: MessageHandler
) = a2aRest(A2A(agentCard, tasks, pushNotifications, subscriptions, messageHandler), basePath)

fun a2aRest(
    cards: AgentCardProvider,
    tasks: TaskStorage = TaskStorage.InMemory(),
    pushNotifications: PushNotificationConfigStorage = PushNotificationConfigStorage.InMemory(),
    subscriptions: TaskSubscriptions = TaskSubscriptions.InMemory(),
    basePath: String = "",
    messageHandler: MessageHandler
) = a2aRest(A2A(cards, tasks, pushNotifications, subscriptions, messageHandler), basePath)

/**
 * Create an A2A server using the HTTP/REST protocol binding.
 */
fun a2aRest(
    protocol: A2A,
    basePath: String = ""
): PolyHandler {
    val capabilities = protocol.cards.extended().capabilities
    val httpHandler = CatchAll()
        .then(CatchLensFailure())
        .then(A2AProtocolNegotiation(capabilities))
        .then(
            routes(
                basePath bind routes(
                    a2aHttpEndpoints(protocol),
                    "{tenant}" bind a2aHttpEndpoints(protocol)
                )
            )
        )

    val sseHandler = basePath bindSse sse(
        a2aSseEndpoints(protocol),
        "{tenant}" bindSse a2aSseEndpoints(protocol)
    )

    return poly(httpHandler, sseHandler)
}

private fun a2aSseEndpoints(protocol: A2A) = "tasks/{taskId}:subscribe" bindSse { req ->
    when (protocol.cards.extended().capabilities.streaming) {
        true -> {
            val taskId = TaskId.of(req.path("taskId")!!)
            val tenant = req.path("tenant")?.let { Tenant.of(it) }
            SseResponse { sse ->
                if (protocol.subscribe(taskId, sse, tenant) == null) {
                    sse.close()
                }
            }
        }

        else -> SseResponse { it.close() }
    }
}

private fun a2aHttpEndpoints(protocol: A2A) = routes(
    "/.well-known/agent-card.json" bind GET to { Response(OK).json(protocol.cards.extended()) },

    "extendedAgentCard" bind GET to {
        when (protocol.cards.extended().capabilities.extendedAgentCard) {
            true -> protocol.cards.extended().let { Response(OK).json(it) }
            else -> Response(BAD_REQUEST)
        }
    },

    "message:send" bind POST to { req ->
        val params = req.json<A2AMessage.Send.Request.Params>().withTenant(req)
        when (val response = protocol.send(params, req)) {
            is Task -> Response(OK).json(response)
            is Message -> Response(OK).json(response)
            is ResponseStream -> error("unreachable")
        }
    },

    "message:stream" bind POST to { req ->
        if (protocol.cards.extended().capabilities.streaming != true) Response(BAD_REQUEST)
        else {
            val json = req.json<A2AMessage.Send.Request.Params>()
            Response(OK)
                .contentType(ContentType.TEXT_EVENT_STREAM)
                .body(
                    protocol.stream(
                        A2AMessage.Stream.Request.Params(
                            json.message,
                            json.configuration,
                            json.metadata,
                            req.tenant()
                        ), req
                    ).toSseStream()
                )
        }
    },

    "tasks" bind routes(
        "{taskId}" bind GET to { req ->
            protocol.getTask(
                A2ATask.Get.Request.Params(
                    taskIdPath(req),
                    historyLengthQuery(req),
                    req.tenant()
                )
            )
                ?.let { Response(OK).json(it) }
                ?: Response(NOT_FOUND)
        },

        "{taskId}:cancel" bind POST to { req ->
            protocol.cancelTask(A2ATask.Cancel.Request.Params(taskIdPath(req), tenant = req.tenant()))
                ?.let { Response(OK).json(it) }
                ?: Response(NOT_FOUND)
        },

        "{taskId}/pushNotificationConfigs" bind POST to { req ->
            when (protocol.cards.extended().capabilities.pushNotifications) {
                true -> {
                    val pathTaskId = taskIdPath(req)
                    val create = req.json<CreateTaskPushNotificationConfig>()
                    if (create.taskId != pathTaskId) Response(BAD_REQUEST)
                    else Response(CREATED).json(
                        protocol.setPushConfig(
                            A2APushNotificationConfig.Set.Request.Params(
                                pathTaskId,
                                create.url,
                                create.token,
                                create.authentication,
                                req.tenant()
                            )
                        )
                    )
                }

                else -> Response(BAD_REQUEST)
            }
        },

        "{taskId}/pushNotificationConfigs/{configId}" bind GET to { req ->
            when (protocol.cards.extended().capabilities.pushNotifications) {
                true -> protocol.getPushConfig(
                    A2APushNotificationConfig.Get.Request.Params(
                        taskIdPath(req),
                        configIdPath(req),
                        req.tenant()
                    )
                )
                    ?.let { Response(OK).json(it) }
                    ?: Response(NOT_FOUND)

                else -> Response(BAD_REQUEST)
            }
        },

        "{taskId}/pushNotificationConfigs" bind GET to { req ->
            when (protocol.cards.extended().capabilities.pushNotifications) {
                true ->
                    Response(OK).json(
                        protocol.listPushConfigs(
                            A2APushNotificationConfig.List.Request.Params(
                                taskIdPath(req),
                                pageSizeQuery(req),
                                pageTokenQuery(req),
                                req.tenant()
                            )
                        )
                    )

                else -> Response(BAD_REQUEST)
            }
        },

        "{taskId}/pushNotificationConfigs/{configId}" bind DELETE to { req ->
            when (protocol.cards.extended().capabilities.pushNotifications) {
                true ->
                    if (protocol.deletePushConfig(
                            A2APushNotificationConfig.Delete.Request.Params(
                                taskIdPath(req),
                                configIdPath(req),
                                req.tenant()
                            )
                        ) != null
                    ) Response(NO_CONTENT)
                    else Response(NOT_FOUND)

                else -> Response(BAD_REQUEST)
            }
        },

        "" bind GET to { req ->
            val page = protocol.listTasks(
                A2ATask.ListTasks.Request.Params(
                    contextId = contextIdQuery(req),
                    status = statusQuery(req),
                    pageSize = pageSizeQuery(req),
                    pageToken = pageTokenQuery(req),
                    historyLength = historyLengthQuery(req),
                    statusTimestampAfter = statusTimestampAfterQuery(req),
                    includeArtifactsQuery(req),
                    req.tenant()
                )
            )
            Response(OK).json(page)
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
private val pageTokenQuery = Query.value(PageToken).optional("pageToken")
private val historyLengthQuery = Query.int().optional("historyLength")
private val statusTimestampAfterQuery = Query.instant().optional("statusTimestampAfter")
private val includeArtifactsQuery = Query.boolean().optional("includeArtifacts")
