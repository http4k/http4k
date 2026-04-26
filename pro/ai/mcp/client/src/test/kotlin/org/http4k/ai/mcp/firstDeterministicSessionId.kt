/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp

import org.http4k.ai.mcp.protocol.SessionId
import org.http4k.ai.mcp.protocol.messages.ClientMessage
import org.http4k.ai.mcp.protocol.messages.McpRpc
import org.http4k.ai.mcp.util.McpJson
import org.http4k.format.renderRequest

val firstDeterministicSessionId = SessionId.parse("8cb4c22c-53fe-ae50-d94e-97b2a94e6b1e")

fun McpJson.renderRequest(hasMethod: McpRpc, input: ClientMessage.Request, id: Int = 1) =
    renderRequest(hasMethod.Method.value, asJsonObject(input), number(id))
