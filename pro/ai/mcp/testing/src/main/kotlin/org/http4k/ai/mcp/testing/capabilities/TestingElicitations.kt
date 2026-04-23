/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.testing.capabilities

import dev.forkhandles.result4k.valueOrNull
import org.http4k.ai.mcp.ElicitationHandler
import org.http4k.ai.mcp.ElicitationRequest
import org.http4k.ai.mcp.ElicitationResponse.Error
import org.http4k.ai.mcp.ElicitationResponse.Ok
import org.http4k.ai.mcp.ElicitationResponse.Task
import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.model.ElicitationId
import org.http4k.ai.mcp.protocol.McpException
import org.http4k.ai.mcp.protocol.messages.DomainError
import org.http4k.ai.mcp.protocol.messages.McpElicitations
import org.http4k.ai.mcp.testing.TestMcpSender
import org.http4k.ai.mcp.testing.nextEvent
import org.http4k.ai.mcp.testing.toNotification
import org.http4k.lens.MetaKey
import org.http4k.lens.progressToken
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
        sender.lastEvent()
            .toNotification<McpElicitations.Complete.Notification.Params>(McpElicitations.Complete)
            .also { onComplete.forEach { it(elicitationId) } }

    init {
        sender.on(McpElicitations) { event ->
            val result = event.nextEvent<McpElicitations.Request.Params, McpElicitations.Request.Params> { this }.valueOrNull()!!
            val (id, protocolRequest) = result

            val domainRequest = when (protocolRequest) {
                is McpElicitations.Request.Params.Form -> ElicitationRequest.Form(
                    protocolRequest.message,
                    protocolRequest.requestedSchema,
                    MetaKey.progressToken<Any>().toLens()(protocolRequest._meta),
                    protocolRequest.task
                )

                is McpElicitations.Request.Params.Url -> ElicitationRequest.Url(
                    protocolRequest.message,
                    protocolRequest.url,
                    protocolRequest.elicitationId,
                    MetaKey.progressToken<Any>().toLens()(protocolRequest._meta),
                    protocolRequest.task
                )
            }

            onElicitation.forEach { handler ->
                val protocolResponse = when (val response = handler(domainRequest)) {
                    is Ok -> McpElicitations.Response.Result(response.action, response.content, _meta = response._meta)
                    is Task -> McpElicitations.Response.Result(task = response.task)
                    is Error -> throw McpException(DomainError(response.message))
                }
                sender(protocolResponse, id!!)
            }
        }
    }
}
