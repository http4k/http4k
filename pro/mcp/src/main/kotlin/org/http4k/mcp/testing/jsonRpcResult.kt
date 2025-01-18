package org.http4k.mcp.testing

import org.http4k.format.AutoMarshallingJson
import org.http4k.jsonrpc.JsonRpcResult
import org.http4k.sse.SseMessage

fun <NODE : Any> AutoMarshallingJson<NODE>.jsonRpcResult(event: SseMessage.Event) =
    JsonRpcResult(this, fields(parse(event.data)).toMap())

