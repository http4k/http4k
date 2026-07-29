/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.server.capability

import org.http4k.ai.mcp.stateless.ElicitationRequest
import org.http4k.ai.mcp.stateless.ElicitationRequest.Form
import org.http4k.ai.mcp.stateless.ElicitationRequest.Url
import org.http4k.ai.mcp.stateless.ElicitationResponse
import org.http4k.ai.mcp.stateless.model.Meta
import org.http4k.ai.mcp.stateless.protocol.ClientCapabilities
import org.http4k.ai.mcp.stateless.protocol.McpException
import org.http4k.ai.mcp.stateless.protocol.messages.McpElicitation
import org.http4k.ai.mcp.stateless.protocol.messages.MissingRequiredClientCapabilityError
import org.http4k.format.MoshiObject
import org.http4k.lens.stateless.MetaKey
import org.http4k.lens.stateless.clientCapabilities

internal fun Map<String, ElicitationRequest>.toWireRequests(capabilities: ClientCapabilities?): Map<String, McpElicitation.Create> {
    val missing = mutableListOf<String>()
    val wire = mapValues { (_, request) ->
        if (capabilities?.elicitation == null) missing += "elicitation"
        when (request) {
            is Form ->
                McpElicitation.Create(McpElicitation.Create.Params.Form(request.message, request.requestedSchema))

            is Url ->
                McpElicitation.Create(McpElicitation.Create.Params.Url(request.message, request.url))
        }
    }
    if (missing.isNotEmpty()) throw McpException(MissingRequiredClientCapabilityError(missing.distinct()))
    return wire
}

internal fun Map<String, McpElicitation.Result>?.toElicitationResponses(): Map<String, ElicitationResponse> =
    orEmpty().mapValues { (_, answer) -> ElicitationResponse.Ok(answer.action, answer.content) }

internal fun Meta.clientCapabilities(): ClientCapabilities? =
    (node.attributes[CLIENT_CAPABILITIES_KEY] as? MoshiObject)
        ?.let { MetaKey.clientCapabilities().toLens()(this) }

private const val CLIENT_CAPABILITIES_KEY = "io.modelcontextprotocol/clientCapabilities"
