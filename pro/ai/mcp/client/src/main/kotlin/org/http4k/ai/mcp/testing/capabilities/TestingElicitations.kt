package org.http4k.ai.mcp.testing.capabilities

import dev.forkhandles.result4k.valueOrNull
import org.http4k.ai.mcp.ElicitationHandler
import org.http4k.ai.mcp.ElicitationRequest
import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.protocol.messages.McpElicitations
import org.http4k.ai.mcp.testing.TestMcpSender
import org.http4k.ai.mcp.testing.nextEvent
import java.time.Duration

class TestingElicitations(sender: TestMcpSender) : McpClient.Elicitations {

    private val onElicitation = mutableListOf<ElicitationHandler>()

    override fun onElicitation(overrideDefaultTimeout: Duration?, fn: ElicitationHandler) {
        onElicitation.add(fn)
    }

    init {
        sender.on(McpElicitations) { event ->
            val (id, req) =
                event.nextEvent<ElicitationRequest, McpElicitations.Request> {
                    ElicitationRequest(message, requestedSchema, _meta.progressToken)
                }.valueOrNull()!!
            onElicitation.forEach { handler ->
                sender(with(handler(req)) { McpElicitations.Response(action, content, _meta) }, id!!)
            }
        }
    }
}
