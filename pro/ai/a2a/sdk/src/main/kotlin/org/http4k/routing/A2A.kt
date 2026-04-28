/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.routing

import org.http4k.ai.a2a.A2ARequest
import org.http4k.ai.a2a.A2AResponse
import org.http4k.ai.a2a.MessageHandler
import org.http4k.ai.a2a.model.AgentCard
import org.http4k.ai.a2a.protocol.messages.A2AJsonRpcRequest
import org.http4k.ai.a2a.server.RoutingA2AHandler
import org.http4k.ai.a2a.server.storage.PushNotificationConfigStorage
import org.http4k.ai.a2a.server.storage.TaskStorage
import org.http4k.ai.a2a.util.A2AJson
import org.http4k.ai.a2a.util.A2AJson.auto
import org.http4k.ai.a2a.util.A2ANodeType
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ServerFilters.CatchAll
import org.http4k.filter.ServerFilters.CatchLensFailure
import org.http4k.lens.contentType
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import kotlin.concurrent.thread

private val agentCardLens = Body.auto<AgentCard>().toLens()
private val jsonRpcLens = Body.auto<A2ANodeType>().toLens()

/**
 * Create an A2A server. Handles both single JSON-RPC responses and SSE streaming
 * based on the request method (message/send vs message/stream).
 */
fun a2a(
    agentCard: AgentCard,
    messageHandler: MessageHandler,
    tasks: TaskStorage = TaskStorage.InMemory(),
    pushNotifications: PushNotificationConfigStorage = PushNotificationConfigStorage.InMemory(),
    rpcPath: String = "/",
    agentCardPath: String = "/.well-known/agent.json"
): HttpHandler {
    val handler = RoutingA2AHandler(messageHandler, tasks, pushNotifications)
    return CatchAll()
        .then(CatchLensFailure())
        .then(
            routes(
                agentCardPath bind GET to { Response(OK).with(agentCardLens of agentCard) },
                rpcPath bind POST to { httpReq ->
                    val message = runCatching { A2AJson.asA<A2AJsonRpcRequest>(httpReq.bodyString()) }
                        .getOrElse { return@to Response(BAD_REQUEST) }

                    when (val result = handler(A2ARequest(message, httpReq))) {
                        is A2AResponse.Single ->
                            Response(OK).with(jsonRpcLens of A2AJson.asJsonObject(result.message))

                        is A2AResponse.Stream ->
                            Response(OK)
                                .contentType(ContentType.TEXT_EVENT_STREAM)
                                .body(result.messages.toSseStream())
                    }
                }
            )
        )
}

private fun Sequence<*>.toSseStream(): InputStream {
    val pipedIn = PipedInputStream()
    val pipedOut = PipedOutputStream(pipedIn)

    thread(isDaemon = true) {
        pipedOut.use { out ->
            for (item in this) {
                val json = with(A2AJson) { asJsonObject(item!!).asCompactJsonString() }
                out.write("data: $json\n\n".toByteArray())
                out.flush()
            }
        }
    }

    return pipedIn
}
