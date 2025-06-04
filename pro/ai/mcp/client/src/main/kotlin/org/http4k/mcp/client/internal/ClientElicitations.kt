package org.http4k.mcp.client.internal

import org.http4k.mcp.ElicitationHandler
import org.http4k.mcp.ElicitationRequest
import org.http4k.mcp.client.McpClient
import org.http4k.mcp.model.McpMessageId
import org.http4k.mcp.protocol.messages.McpElicitations
import org.http4k.mcp.protocol.messages.McpRpc
import java.time.Duration

internal class ClientElicitations(
    private val tidyUp: (McpMessageId) -> Unit,
    private val defaultTimeout: Duration,
    private val sender: McpRpcSender,
    private val register: (McpRpc, McpCallback<*>) -> Any
) : McpClient.Elicitations {

    override fun onElicitation(overrideDefaultTimeout: Duration?, fn: ElicitationHandler) {
        register(McpElicitations, McpCallback(McpElicitations.Request::class) { request, requestId ->
            if (requestId == null) return@McpCallback

            val response = fn(ElicitationRequest(request.message, request.requestedSchema, request._meta.progress))

            val timeout = overrideDefaultTimeout ?: defaultTimeout

            sender(
                McpElicitations,
                McpElicitations.Response(response.action, response.content, response._meta),
                timeout,
                requestId
            )

            tidyUp(requestId)
        })
    }
}
