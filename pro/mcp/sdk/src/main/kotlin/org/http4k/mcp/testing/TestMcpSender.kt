package org.http4k.mcp.testing

import org.http4k.core.PolyHandler
import org.http4k.core.Request
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.format.renderRequest
import org.http4k.format.renderResult
import org.http4k.mcp.protocol.messages.ClientMessage
import org.http4k.mcp.protocol.messages.McpRpc
import org.http4k.mcp.util.McpJson
import java.util.concurrent.atomic.AtomicReference

class TestMcpSender(private val poly: PolyHandler, private val messageRequest: AtomicReference<Request>) {
    operator fun invoke(hasMethod: McpRpc, input: ClientMessage.Request) {
        this(with(McpJson) {
            compact(renderRequest(hasMethod.Method.value, asJsonObject(input), number(1)))
        })
    }

    operator fun invoke(input: ClientMessage.Response) {
        this(with(McpJson) {
            compact(renderResult(asJsonObject(input), number(1)))
        })
    }

    operator fun invoke(hasMethod: McpRpc, input: ClientMessage.Notification) {
        this(with(McpJson) {
            compact(renderRequest(hasMethod.Method.value, asJsonObject(input), number(1)))
        })
    }

    operator fun invoke(body: String) {
        val response = poly.http!!(messageRequest.get().body(body))
        require(response.status == ACCEPTED, { "Failed to send MCP request: ${response.status}" })
    }
}
