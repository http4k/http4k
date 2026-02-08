package org.http4k.ai.mcp.server.http

import org.http4k.ai.mcp.server.protocol.ClientRequestContext.Subscription
import org.http4k.ai.mcp.server.protocol.InvalidSession
import org.http4k.ai.mcp.server.protocol.McpProtocol
import org.http4k.ai.mcp.server.protocol.Session
import org.http4k.ai.mcp.util.asHttp
import org.http4k.core.ContentType
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.lens.Header
import org.http4k.lens.MCP_SESSION_ID
import org.http4k.lens.contentType
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.sse.Sse
import org.http4k.sse.SseMessage

/**
 * Routes inbound POST requests to the MCP server to the MCP protocol for processing (returning responses via JSON RPC),
 * and deletes old sessions at the request of the client.
 */
fun HttpNonStreamingMcpConnection(protocol: McpProtocol<Sse>, path: String = "/mcp") =
    path bind routes(
        POST to { req ->
            with(protocol) {
                when (val session = retrieveSession(req)) {
                    is Session -> {
                        receive(FakeSse(req), session, req).asHttp(OK)
                            .with(Header.MCP_SESSION_ID of session.id)
                    }

                    is InvalidSession -> Response(NOT_FOUND)
                }
            }
        },
        GET to { req ->
            with(protocol) {
                when (val session = retrieveSession(req)) {
                    is Session -> Response(OK).contentType(ContentType.TEXT_EVENT_STREAM)
                        .with(Header.MCP_SESSION_ID of session.id)

                    is InvalidSession -> Response(NOT_FOUND)
                }
            }
        },
        DELETE to { req ->
            with(protocol) {
                when (val session = retrieveSession(req)) {
                    is Session -> {
                        end(Subscription(session))
                        Response(OK).contentType(ContentType.TEXT_EVENT_STREAM)
                            .with(Header.MCP_SESSION_ID of session.id)
                    }

                    is InvalidSession -> Response(NOT_FOUND)
                }
            }
        }
    )

private class FakeSse(override val connectRequest: Request) : Sse {
    override fun send(message: SseMessage) = this
    override fun close() {}
    override fun onClose(fn: () -> Unit): Sse = this
}

