/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.routing

import org.http4k.ai.a2a.MessageHandler
import org.http4k.ai.a2a.MessageResponse
import org.http4k.ai.a2a.MessageResponse.Message
import org.http4k.ai.a2a.MessageResponse.Task
import org.http4k.ai.a2a.model.AgentCard
import org.http4k.ai.a2a.model.AgentCardProvider
import org.http4k.ai.a2a.model.ContextId
import org.http4k.ai.a2a.model.PushNotificationConfig
import org.http4k.ai.a2a.model.PushNotificationConfigId
import org.http4k.ai.a2a.model.TaskId
import org.http4k.ai.a2a.model.TaskState
import org.http4k.ai.a2a.protocol.messages.A2AMessage
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
) =
    CatchAll()
        .then(CatchLensFailure())
        .then(
            routes(
                basePath bind
                    routes(
                        "/.well-known/agent-card.json" bind GET to { Response(OK).json(protocol.cards.standard()) },

                        "extendedAgentCard" bind GET to { protocol.cards.extended().let { Response(OK).json(it) } },

                        "message:send" bind POST to { req ->
                            val params = req.json<A2AMessage.Send.Request.Params>()
                            val returnImmediately = params.configuration?.returnImmediately == true
                            when (val response = protocol.send(params.message, req)) {
                                is Task -> Response(OK).json(if (returnImmediately) response.tasks.first() else response.tasks.last())
                                is Message -> Response(OK).json(response.message)
                            }
                        },

                        "message:stream" bind POST to { req ->
                            val params = req.json<A2AMessage.Send.Request.Params>()
                            val response = protocol.send(params.message, req)
                            Response(OK)
                                .contentType(ContentType.TEXT_EVENT_STREAM)
                                .body(response.toRestSseStream())
                        },

                        "tasks" bind routes(
                            "{taskId}" bind GET to { req ->
                                protocol.getTask(taskIdPath(req))
                                    ?.let { Response(OK).json(it) }
                                    ?: Response(NOT_FOUND)
                            },

                            "{taskId}:cancel" bind POST to { req ->
                                protocol.cancelTask(taskIdPath(req))
                                    ?.let { Response(OK).json(it) }
                                    ?: Response(NOT_FOUND)
                            },

                            "{taskId}/pushNotificationConfigs" bind POST to { req ->
                                Response(CREATED).json(
                                    protocol.setPushConfig(
                                        taskIdPath(req),
                                        req.json<PushNotificationConfig>()
                                    )
                                )
                            },

                            "{taskId}/pushNotificationConfigs/{configId}" bind GET to { req ->
                                protocol.getPushConfig(configIdPath(req))
                                    ?.let { Response(OK).json(it) }
                                    ?: Response(NOT_FOUND)
                            },

                            "{taskId}/pushNotificationConfigs" bind GET to { req ->
                                Response(OK).json(protocol.listPushConfigs(taskIdPath(req)))
                            },

                            "{taskId}/pushNotificationConfigs/{configId}" bind DELETE to { req ->
                                if (protocol.deletePushConfig(configIdPath(req)) != null) Response(NO_CONTENT)
                                else Response(NOT_FOUND)
                            },

                            "" bind GET to { req ->
                                Response(OK).json(
                                    protocol.listTasks(
                                        contextId = contextIdQuery(req),
                                        status = statusQuery(req),
                                        pageSize = pageSizeQuery(req),
                                        pageToken = pageTokenQuery(req)
                                    )
                                )
                            }
                        )
                    )
            )
        )

private val taskIdPath = Path.value(TaskId).of("taskId")
private val configIdPath = Path.value(PushNotificationConfigId).of("configId")
private val contextIdQuery = Query.value(ContextId).optional("contextId")
private val statusQuery = Query.enum<TaskState>().optional("status")
private val pageSizeQuery = Query.int().optional("pageSize")
private val pageTokenQuery = Query.string().optional("pageToken")

private fun MessageResponse.toRestSseStream(): InputStream {
    val pipedIn = PipedInputStream()
    val pipedOut = PipedOutputStream(pipedIn)

    thread(isDaemon = true) {
        pipedOut.use { out ->
            when (this) {
                is Task -> tasks.forEach { task ->
                    out.write(SseMessage.Event("task", A2AJson.asFormatString(task)).toMessage().toByteArray())
                    out.flush()
                }

                is Message -> {
                    out.write(SseMessage.Event("message", A2AJson.asFormatString(message)).toMessage().toByteArray())
                    out.flush()
                }
            }
        }
    }

    return pipedIn
}
