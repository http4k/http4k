/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.http

import org.http4k.ai.mcp.server.asHttp
import org.http4k.ai.mcp.server.protocol.ClientRequestContext.Subscription
import org.http4k.ai.mcp.server.protocol.McpProtocol
import org.http4k.ai.mcp.server.protocol.McpSessionState.Invalid
import org.http4k.ai.mcp.server.protocol.McpSessionState.Valid
import org.http4k.core.ContentType
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.METHOD_NOT_ALLOWED
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.lens.ALLOW
import org.http4k.lens.Header
import org.http4k.lens.MCP_SESSION_ID
import org.http4k.lens.contentType
import org.http4k.routing.asRouter
import org.http4k.routing.bind
import org.http4k.routing.orElse
import org.http4k.routing.routes
import org.http4k.sse.Sse
import org.http4k.sse.SseMessage

/**
 * Routes inbound POST requests to the MCP server to the MCP protocol for processing (returning responses via JSON RPC),
 * and deletes old sessions at the request of the client.
 */
fun HttpNonStreamingMcpConnection(protocol: McpProtocol<Sse>, path: String = "/mcp") =
    path bind routes(
        POST.asRouter() bind { req ->
            with(protocol) {
                when (val sessionState = retrieveSession(req)) {
                    is Valid
 ->
                        receive(FakeSse(req), sessionState, req).asHttp(OK)
                            .with(Header.MCP_SESSION_ID of sessionState.session.id)

                    Invalid -> Response(NOT_FOUND)
                }
            }
        },
        GET.asRouter() bind { req ->
            with(protocol) {
                when (val sessionState = retrieveSession(req)) {
                    is Valid
 -> Response(OK).contentType(ContentType.TEXT_EVENT_STREAM)
                        .with(Header.MCP_SESSION_ID of sessionState.session.id)

                    Invalid -> Response(NOT_FOUND)
                }
            }
        },
        DELETE.asRouter() bind { req: Request ->
            with(protocol) {
                when (val sessionState = retrieveSession(req)) {
                    is Valid
 -> {
                        unsubscribe(Subscription(sessionState.session))
                        Response(ACCEPTED).contentType(ContentType.TEXT_EVENT_STREAM)
                            .with(Header.MCP_SESSION_ID of sessionState.session.id)
                    }

                    Invalid -> Response(NOT_FOUND)
                }
            }
        },
        orElse bind { Response(METHOD_NOT_ALLOWED).with(Header.ALLOW of listOf(GET, POST, DELETE)) }
    )

private class FakeSse(override val connectRequest: Request) : Sse {
    override fun send(message: SseMessage) = this
    override fun close() {}
    override fun onClose(fn: () -> Unit): Sse = this
}

