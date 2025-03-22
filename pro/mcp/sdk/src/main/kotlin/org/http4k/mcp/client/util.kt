package org.http4k.mcp.client

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.mapFailure
import dev.forkhandles.result4k.resultFrom
import org.http4k.format.MoshiNode
import org.http4k.format.MoshiObject
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidRequest
import org.http4k.jsonrpc.ErrorMessage.Companion.ParseError
import org.http4k.mcp.protocol.messages.ServerMessage
import org.http4k.mcp.util.McpJson
import org.http4k.sse.SseMessage.Event

internal inline fun <reified T : ServerMessage> Event.asAOrFailure(): Result<T, McpError.Protocol> = with(McpJson) {
    val data = parse(data) as MoshiObject

    when {
        data["method"] != null -> Failure(McpError.Protocol(InvalidRequest))
        else -> {
            resultFrom {
                convert<MoshiNode, T>(data.attributes["result"] ?: nullNode())
            }.mapFailure { McpError.Protocol(ParseError) }
        }
    }
}
