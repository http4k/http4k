/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.http

import org.http4k.ai.mcp.server.protocol.ClientRequestContext.Subscription
import org.http4k.ai.mcp.server.protocol.McpProtocol
import org.http4k.ai.mcp.server.protocol.McpSessionState.Invalid
import org.http4k.ai.mcp.server.protocol.McpSessionState.Valid
import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.METHOD_NOT_ALLOWED
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.accepted
import org.http4k.lens.ALLOW
import org.http4k.lens.Header
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.lens.MCP_SESSION_ID
import org.http4k.lens.X_ACCEL_BUFFERING
import org.http4k.lens.XAccelBuffering
import org.http4k.routing.sse
import org.http4k.routing.sse.bind
import org.http4k.sse.Sse
import org.http4k.sse.SseMessage.Event
import org.http4k.sse.SseResponse

fun HttpStreamingMcpConnection(protocol: McpProtocol<Sse>, path: String = "/mcp") =
    path bind sse(TEXT_EVENT_STREAM.accepted() bind { req: Request ->

        when (req.method) {
            GET, POST, DELETE -> when (val sessionState = protocol.retrieveSession(req)) {
                is Valid -> SseResponse(
                    OK, listOf(
                        CONTENT_TYPE.meta.name to TEXT_EVENT_STREAM.withNoDirectives().value,
                        Header.MCP_SESSION_ID.meta.name to sessionState.session.id.value,
                        Header.X_ACCEL_BUFFERING.meta.name to XAccelBuffering.no.name,
                    )
                ) { sse ->
                    with(protocol) {
                        val subscription = Subscription(sessionState.session)
                        when (req.method) {
                            GET -> {
                                protocol.subscribe(subscription, sse, req)
                                sse.send(Event("ping", ""))
                            }

                            POST -> sse.use { receive(it, sessionState, req) }
                            else -> {
                                unsubscribe(subscription)
                                sse.close()
                            }
                        }
                    }
                }

                Invalid -> SseResponse(NOT_FOUND) { it.close() }
            }

            else -> SseResponse(METHOD_NOT_ALLOWED,
                listOf(Header.ALLOW.meta.name to listOf(GET, POST, DELETE).joinToString(", "))
            ) { it.close() }
        }
    })
