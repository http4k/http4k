package org.http4k.mcp.server.http

import org.http4k.core.Method.DELETE
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.mcp.server.protocol.ClientRequestContext.Subscription
import org.http4k.mcp.server.protocol.InvalidSession
import org.http4k.mcp.server.protocol.McpProtocol
import org.http4k.mcp.server.protocol.Session
import org.http4k.mcp.util.asHttp
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.sse.Sse
import org.http4k.sse.SseMessage

/**
 * Routes inbound POST requests to the MCP server to the MCP protocol for processing (returning responses via JSON RPC),
 * and deletes old sessions at the request of the client.
 */
fun HttpNonStreamingMcpConnection(protocol: McpProtocol<Sse>) =
    "/mcp" bind routes(
        POST to { req ->
            with(protocol) {
                when (val session = retrieveSession(req)) {
                    is Session -> receive(FakeSse(req), session, req).asHttp()
                    is InvalidSession -> Response(NOT_FOUND)
                }
            }
        },
        DELETE to { req ->
            when (val session = protocol.retrieveSession(req)) {
                is Session -> {
                    protocol.end(Subscription(session))
                    Response(ACCEPTED)
                }

                InvalidSession -> Response(NOT_FOUND)
            }
        }
    )

private class FakeSse(override val connectRequest: Request) : Sse {
    override fun send(message: SseMessage) = this
    override fun close() {}
    override fun onClose(fn: () -> Unit): Sse = this
}

