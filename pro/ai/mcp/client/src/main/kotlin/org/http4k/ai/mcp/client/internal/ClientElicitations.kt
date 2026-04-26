/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.client.internal

import org.http4k.ai.mcp.ElicitationHandler
import org.http4k.ai.mcp.ElicitationRequest
import org.http4k.ai.mcp.ElicitationResponse
import org.http4k.ai.mcp.ElicitationResponse.Error
import org.http4k.ai.mcp.ElicitationResponse.Ok
import org.http4k.ai.mcp.ElicitationResponse.Task
import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.model.ElicitationId
import org.http4k.ai.mcp.model.McpMessageId
import org.http4k.ai.mcp.protocol.McpException
import org.http4k.ai.mcp.protocol.messages.DomainError
import org.http4k.ai.mcp.protocol.messages.McpElicitations
import org.http4k.ai.mcp.protocol.messages.McpElicitations.Request.Params.Form
import org.http4k.ai.mcp.protocol.messages.McpElicitations.Request.Params.Url
import org.http4k.ai.mcp.util.McpJson
import org.http4k.lens.MetaKey
import org.http4k.lens.progressToken
import java.time.Duration

internal class ClientElicitations(
    private val tidyUp: (McpMessageId) -> Unit,
    private val defaultTimeout: Duration,
    private val sender: McpRpcSender,
    private val register: McpCallbackRegistry
) : McpClient.Elicitations {

    override fun onComplete(fn: (ElicitationId) -> Unit) {
        register.on(McpElicitations.Complete.Notification::class) { notification, _ ->
            fn(notification.params.elicitationId)
        }
    }

    override fun onElicitation(overrideDefaultTimeout: Duration?, fn: ElicitationHandler) {
        register.on(McpElicitations.Request::class) { req, requestId ->
            if (requestId == null) return@on
            val response = when (val request = req.params) {
                is Form -> fn(
                    ElicitationRequest.Form(
                        request.message,
                        request.requestedSchema,
                        MetaKey.progressToken<Any>().toLens()(request._meta),
                        request.task
                    )
                )

                is Url -> fn(
                    ElicitationRequest.Url(
                        request.message,
                        request.url,
                        request.elicitationId,
                        MetaKey.progressToken<Any>().toLens()(request._meta),
                        request.task
                    )
                )
            }

            val timeout = overrideDefaultTimeout ?: defaultTimeout

            sender(
                McpElicitations.Response(response.toProtocol(), requestId),
                timeout,
                requestId
            )

            tidyUp(requestId)
        }
    }
}

private fun ElicitationResponse.toProtocol() = when (this) {
    is Ok -> McpElicitations.Response.Result(action, content, _meta = _meta)
    is Task -> McpElicitations.Response.Result(content = McpJson.nullNode(), task = task)
    is Error -> throw McpException(DomainError(message))
}

