package org.http4k.mcp

import org.http4k.format.renderRequest
import org.http4k.mcp.protocol.SessionId
import org.http4k.mcp.protocol.messages.ClientMessage
import org.http4k.mcp.protocol.messages.McpRpc
import org.http4k.mcp.util.McpJson

val firstDeterministicSessionId = SessionId.parse("8cb4c22c-53fe-ae50-d94e-97b2a94e6b1e")

fun McpJson.renderRequest(hasMethod: McpRpc, input: ClientMessage.Request, id: Int = 1) =
    renderRequest(hasMethod.Method.value, asJsonObject(input), number(id))
