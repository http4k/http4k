/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

import org.http4k.ai.mcp.Client.Companion.NoOp
import org.http4k.ai.mcp.protocol.McpException
import org.http4k.ai.mcp.protocol.ProtocolVersion.Companion.DRAFT
import org.http4k.ai.mcp.protocol.messages.HeaderMismatchError
import org.http4k.ai.mcp.protocol.messages.McpCancelled
import org.http4k.ai.mcp.protocol.messages.McpCompletion
import org.http4k.ai.mcp.protocol.messages.McpInitialize
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcEmptyResponse
import org.http4k.ai.mcp.protocol.messages.McpLogging
import org.http4k.ai.mcp.protocol.messages.McpProgress
import org.http4k.ai.mcp.protocol.messages.McpPrompt
import org.http4k.ai.mcp.protocol.messages.McpResource
import org.http4k.ai.mcp.protocol.messages.McpTool
import org.http4k.ai.mcp.server.protocol.ClientRequestContext.Subscription
import org.http4k.ai.mcp.util.McpJson
import org.http4k.format.unwrap
import org.http4k.lens.Header
import org.http4k.lens.MCP_NAME

// ponytail: server→client push removed for the stateless model — capability handlers now receive
// Client.NoOp (they call back via MRTR InputRequired instead, added in Stage 4).
fun RoutingMcpHandler(
    initializer: Initializer,
    clientTracking: MutableMap<Session, ClientTracking>,
    completions: Completions,
    prompts: Prompts,
    resources: Resources,
    tools: Tools,
    cancellations: Cancellations,
    sessions: Sessions<*>,
): McpHandler {
    fun McpRequest.isDraftProtocol() =
        clientTracking[session]?.let { it.protocolVersion >= DRAFT } == true

    fun McpRequest.validateMcpName(bodyName: String) = when {
        isDraftProtocol() && Header.MCP_NAME(http) != bodyName ->
            throw McpException(HeaderMismatchError("Mcp-Name header value does not match body value"))

        else -> null
    }

    return ValidateMcpMethodHeader(clientTracking).then { mcp ->
        when (mcp.message) {
            is McpInitialize.Request -> {
                val initialize = initializer(mcp.message.params, mcp.http)
                clientTracking[mcp.session] = ClientTracking(mcp.message.params)
                McpResponse.Ok(McpInitialize.Response(initialize, mcp.message.id?.coerce()))
            }

            is McpCompletion.Request -> McpResponse.Ok(
                McpCompletion.Response(
                    completions.complete(mcp.message.params, NoOp, mcp.http), mcp.message.id?.coerce()
                )
            )

            is McpPrompt.Get.Request -> mcp.validateMcpName(mcp.message.params.name.value) ?: McpResponse.Ok(
                McpPrompt.Get.Response(
                    prompts.get(mcp.message.params, NoOp, mcp.http), mcp.message.id?.coerce()
                )
            )

            is McpPrompt.List.Request -> McpResponse.Ok(
                McpPrompt.List.Response(
                    prompts.list(mcp.message.params ?: McpPrompt.List.Request.Params(), NoOp, mcp.http),
                    mcp.message.id?.coerce()
                )
            )

            is McpResource.ListTemplates.Request -> McpResponse.Ok(
                McpResource.ListTemplates.Response(
                    resources.listTemplates(
                        mcp.message.params ?: McpResource.ListTemplates.Request.Params(), NoOp, mcp.http
                    ), mcp.message.id?.coerce()
                )
            )

            is McpResource.List.Request -> McpResponse.Ok(
                McpResource.List.Response(
                    resources.listResources(
                        mcp.message.params ?: McpResource.List.Request.Params(), NoOp, mcp.http
                    ), mcp.message.id?.coerce()
                )
            )

            is McpResource.Read.Request -> mcp.validateMcpName(mcp.message.params.uri.toString()) ?: McpResponse.Ok(
                McpResource.Read.Response(
                    resources.read(mcp.message.params, NoOp, mcp.http), mcp.message.id?.coerce()
                )
            )

            is McpResource.Subscribe.Request -> {
                if (resources is ObservableResources) {
                    resources.subscribe(mcp.session, mcp.message.params) {
                        sessions.send(
                            Subscription(mcp.session),
                            McpResource.Updated.Notification(
                                McpResource.Updated.Notification.Params(mcp.message.params.uri)
                            )
                        )
                    }
                }
                McpResponse.Ok(McpJsonRpcEmptyResponse(mcp.message.id?.coerce()))
            }

            is McpResource.Unsubscribe.Request -> {
                if (resources is ObservableResources) resources.unsubscribe(mcp.session, mcp.message.params)
                McpResponse.Ok(McpJsonRpcEmptyResponse(mcp.message.id?.coerce()))
            }

            is McpTool.Call.Request -> mcp.validateMcpName(mcp.message.params.name.value) ?: McpResponse.Ok(
                McpTool.Call.Response(
                    tools.call(mcp.message.params, NoOp, mcp.http), mcp.message.id?.coerce()
                )
            )

            is McpTool.List.Request -> McpResponse.Ok(
                McpTool.List.Response(
                    tools.list(mcp.message.params ?: McpTool.List.Request.Params(), NoOp, mcp.http),
                    mcp.message.id?.coerce()
                )
            )

            is McpInitialize.Initialized.Notification -> McpResponse.Accepted

            is McpProgress.Notification -> McpResponse.Accepted

            is McpCancelled.Notification -> {
                cancellations.cancel(mcp.message.params)
                McpResponse.Accepted
            }

            is McpPrompt.List.Changed.Notification -> McpResponse.Accepted

            is McpTool.List.Changed.Notification -> McpResponse.Accepted

            is McpResource.List.Changed.Notification -> McpResponse.Accepted

            is McpResource.Updated.Notification -> McpResponse.Accepted

            is McpLogging.LoggingMessage.Notification -> McpResponse.Accepted
        }
    }
}

private fun Any.coerce(): Any? = McpJson.asJsonObject(this).unwrap()
