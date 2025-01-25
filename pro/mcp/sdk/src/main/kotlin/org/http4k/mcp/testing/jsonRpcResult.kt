package org.http4k.mcp.testing

import org.http4k.jsonrpc.JsonRpcResult
import org.http4k.mcp.util.McpJson
import org.http4k.sse.SseMessage

fun McpJson.jsonRpcResult(event: SseMessage.Event) =
    JsonRpcResult(this, fields(parse(event.data)).toMap())

