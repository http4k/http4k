package org.http4k.mcp.testing

import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.Method.GET
import org.http4k.core.PolyHandler
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.lens.Header
import org.http4k.lens.MCP_SESSION_ID
import org.http4k.lens.accept
import org.http4k.mcp.model.McpMessageId
import org.http4k.mcp.protocol.SessionId
import org.http4k.mcp.protocol.messages.ClientMessage
import org.http4k.mcp.protocol.messages.McpRpc
import org.http4k.sse.SseMessage
import org.http4k.testing.testSseClient
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class TestMcpSender(private val poly: PolyHandler, private val connectRequest: Request) {

    private var id = AtomicInteger(0)

    private var sessionId = AtomicReference<SessionId>()

    fun stream() = poly.callWith(connectRequest.accept(TEXT_EVENT_STREAM).method(GET))

    operator fun invoke(mcpRpc: McpRpc, input: ClientMessage.Request) =
        poly.callWith(connectRequest.withMcp(mcpRpc, input, id.incrementAndGet()))

    operator fun invoke(mcpRpc: McpRpc, input: ClientMessage.Notification) =
        poly.callWith(connectRequest.withMcp(mcpRpc, input, id.incrementAndGet()))

    operator fun invoke(input: ClientMessage.Response, id: McpMessageId) =
        poly.callWith(connectRequest.withMcp(input, id))

    private fun PolyHandler.callWith(request: Request): Sequence<SseMessage.Event> {
        val client = when (sessionId.get()) {
            null -> testSseClient(request)
            else -> testSseClient(request.with(Header.MCP_SESSION_ID of sessionId.get()))
        }

        val newSessionId = Header.MCP_SESSION_ID(client.response)

        if (sessionId.get() == null && newSessionId != null) {
            sessionId.set(Header.MCP_SESSION_ID(client.response))
        }

        require(client.response.status == OK)

        return client.received().filterIsInstance<SseMessage.Event>().filter { it.event == "message" }
    }
}
