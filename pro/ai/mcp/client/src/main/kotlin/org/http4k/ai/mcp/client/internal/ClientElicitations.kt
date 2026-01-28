package org.http4k.ai.mcp.client.internal

import org.http4k.ai.mcp.ElicitationHandler
import org.http4k.ai.mcp.ElicitationRequest
import org.http4k.ai.mcp.ElicitationResponse
import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.model.ElicitationId
import org.http4k.ai.mcp.model.McpMessageId
import org.http4k.ai.mcp.protocol.messages.McpElicitations
import org.http4k.ai.mcp.protocol.messages.McpRpc
import org.http4k.ai.mcp.util.McpJson
import java.time.Duration

internal class ClientElicitations(
    private val tidyUp: (McpMessageId) -> Unit,
    private val defaultTimeout: Duration,
    private val sender: McpRpcSender,
    private val register: (McpRpc, McpCallback<*>) -> Any
) : McpClient.Elicitations {

    override fun onComplete(fn: (ElicitationId) -> Unit) {
        register(
            McpElicitations.Complete,
            McpCallback(McpElicitations.Complete.Notification::class) { notification, _ ->
                fn(notification.elicitationId)
            })
    }

    override fun onElicitation(overrideDefaultTimeout: Duration?, fn: ElicitationHandler) {
        register(McpElicitations,
            McpCallback(McpElicitations.Request.Form::class) { request, requestId ->
                if (requestId == null) return@McpCallback

                val response = fn(
                    ElicitationRequest.Form(
                        request.message,
                        request.requestedSchema,
                        request._meta.progressToken,
                        request.task
                    )
                )

                val timeout = overrideDefaultTimeout ?: defaultTimeout

                sender(
                    McpElicitations,
                    response.toProtocol(),
                    timeout,
                    requestId
                )

                tidyUp(requestId)
            })

        register(
            McpElicitations,
            McpCallback(McpElicitations.Request.Url::class) { request, requestId ->
                if (requestId == null) return@McpCallback

                val response = fn(
                    ElicitationRequest.Url(
                        request.message,
                        request.url,
                        request.elicitationId,
                        request._meta.progressToken,
                        request.task
                    )
                )

                val timeout = overrideDefaultTimeout ?: defaultTimeout

                sender(
                    McpElicitations,
                    response.toProtocol(),
                    timeout,
                    requestId
                )

                tidyUp(requestId)
            })
    }
}

private fun ElicitationResponse.toProtocol() = when (this) {
    is ElicitationResponse.Ok -> McpElicitations.Response(action, content, _meta = _meta)
    is ElicitationResponse.Task -> McpElicitations.Response(content = McpJson.nullNode(), task = task)
}

