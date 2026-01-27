package org.http4k.ai.mcp.testing.capabilities

import dev.forkhandles.result4k.valueOrNull
import org.http4k.ai.mcp.ElicitationHandler
import org.http4k.ai.mcp.ElicitationRequest
import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.model.ElicitationId
import org.http4k.ai.mcp.protocol.messages.McpElicitations
import org.http4k.ai.mcp.testing.TestMcpSender
import org.http4k.ai.mcp.testing.nextEvent
import org.http4k.ai.mcp.testing.nextNotification
import java.time.Duration

class TestingElicitations(private val sender: TestMcpSender) : McpClient.Elicitations {

    private val onElicitation = mutableListOf<ElicitationHandler>()
    private val onComplete = mutableListOf<(ElicitationId) -> Unit>()

    override fun onElicitation(overrideDefaultTimeout: Duration?, fn: ElicitationHandler) {
        onElicitation.add(fn)
    }

    override fun onComplete(fn: (ElicitationId) -> Unit) {
        onComplete.add(fn)
    }

    fun expectCompleteNotification(elicitationId: ElicitationId) =
        sender.stream().nextNotification<McpElicitations.Complete.Notification>(McpElicitations.Complete)
            .also { onComplete.forEach { it(elicitationId) } }

    init {
        sender.on(McpElicitations) { event ->
            val result = event.nextEvent<McpElicitations.Request, McpElicitations.Request> { this }.valueOrNull()!!
            val (id, protocolRequest) = result

            val domainRequest = when (protocolRequest) {
                is McpElicitations.Request.Form -> ElicitationRequest.Form(
                    protocolRequest.message,
                    protocolRequest.requestedSchema,
                    protocolRequest._meta.progressToken
                )

                is McpElicitations.Request.Url -> ElicitationRequest.Url(
                    protocolRequest.message,
                    protocolRequest.url,
                    protocolRequest.elicitationId,
                    protocolRequest._meta.progressToken
                )
            }

            onElicitation.forEach { handler ->
                sender(with(handler(domainRequest)) { McpElicitations.Response(action, content, _meta) }, id!!)
            }
        }
    }
}
