package org.http4k.ai.mcp.client.internal

import org.http4k.ai.mcp.ElicitationHandler
import org.http4k.ai.mcp.ElicitationRequest
import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.model.ElicitationId
import org.http4k.ai.mcp.model.McpMessageId
import org.http4k.ai.mcp.protocol.messages.McpElicitations
import org.http4k.ai.mcp.protocol.messages.McpRpc
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
                        request._meta.progressToken
                    )
                )

                val timeout = overrideDefaultTimeout ?: defaultTimeout

                sender(
                    McpElicitations,
                    McpElicitations.Response(response.action, response.content, response._meta),
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
                        request._meta.progressToken
                    )
                )

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
