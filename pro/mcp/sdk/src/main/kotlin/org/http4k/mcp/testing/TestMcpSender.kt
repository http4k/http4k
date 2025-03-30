package org.http4k.mcp.testing

import org.http4k.core.PolyHandler
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.mcp.protocol.messages.ClientMessage
import org.http4k.mcp.protocol.messages.McpRpc
import org.http4k.testing.TestSseClient
import org.http4k.testing.testSseClient
import java.util.concurrent.atomic.AtomicInteger

class TestMcpSender(private val poly: PolyHandler, private val connectRequest: Request) {

    private var id = AtomicInteger(0)

    operator fun invoke(mcpRpc: McpRpc, input: ClientMessage.Request): TestSseClient {
        val client = poly.testSseClient(
            connectRequest.withMcp(mcpRpc, input, id.incrementAndGet())
        )

        require(client.status == Status.OK)
        return client
    }

    operator fun invoke(mcpRpc: McpRpc, input: ClientMessage.Notification): TestSseClient {
        val client = poly.testSseClient(
            connectRequest.withMcp(mcpRpc, input, id.incrementAndGet())
        )

        require(client.status == Status.OK)
        return client
    }
}
