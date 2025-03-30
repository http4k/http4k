package org.http4k.mcp.testing

import org.http4k.core.ContentType
import org.http4k.core.PolyHandler
import org.http4k.core.Request
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.format.renderRequest
import org.http4k.format.renderResult
import org.http4k.lens.accept
import org.http4k.mcp.model.McpMessageId
import org.http4k.mcp.protocol.messages.ClientMessage
import org.http4k.mcp.protocol.messages.McpRpc
import org.http4k.mcp.util.McpJson
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class TestMcpSender(private val poly: PolyHandler, private val messageRequest: AtomicReference<Request>) {
    private var id = AtomicInteger(0)

    operator fun invoke(mcpRpc: McpRpc, input: ClientMessage.Request) {
        this(messageRequest.get().withMcp(mcpRpc, input, id.incrementAndGet()))
    }

    operator fun invoke(input: ClientMessage.Response, messageId: McpMessageId) {
        this(messageRequest.get().withMcp(input, messageId))
    }

    operator fun invoke(hasMethod: McpRpc, input: ClientMessage.Notification) {
        this(messageRequest.get().withMcp(hasMethod, input, id.incrementAndGet()))
    }

    operator fun invoke(request: Request) {
        val response = poly.http!!(request)
        require(response.status == ACCEPTED, { "Failed to send MCP request: ${response.status}" })
    }
}

fun Request.withMcp(mcpRpc: McpRpc, input: ClientMessage.Request, id: Int) =
    with(McpJson) {
        accept(ContentType.TEXT_EVENT_STREAM)
            .body(
                compact(
                    renderRequest(
                        mcpRpc.Method.value,
                        asJsonObject(input),
                        number(id)
                    )
                )
            )
    }

fun Request.withMcp(input: ClientMessage.Response, messageId: McpMessageId) =
        with(McpJson) {
            accept(ContentType.TEXT_EVENT_STREAM)
                .body(compact(renderResult(asJsonObject(input), number(messageId.value))))
    }

// TODO remove IDs
fun Request.withMcp(mcpRpc: McpRpc, input: ClientMessage.Notification, id: Int) =
    with(McpJson) {
        accept(ContentType.TEXT_EVENT_STREAM)
            .body(
                compact(
                    renderRequest(
                        mcpRpc.Method.value,
                        asJsonObject(input),
                        number(id)
                    )
                )
            )
    }
