package org.http4k.mcp.testing

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.jsonrpc.JsonRpcResult
import org.http4k.mcp.client.McpError
import org.http4k.mcp.client.McpResult
import org.http4k.mcp.util.McpJson
import org.http4k.mcp.util.McpNodeType
import org.http4k.sse.SseMessage
import org.http4k.testing.TestSseClient
import java.util.concurrent.atomic.AtomicReference

inline fun <reified T : Any, OUT> AtomicReference<TestSseClient>.nextEvent(fn: T.() -> OUT): McpResult<OUT> {
    val jsonRpcResult = JsonRpcResult(
        McpJson,
        McpJson.fields(McpJson.parse((get().received().first() as SseMessage.Event).data)).toMap()
    )

    return when {
        jsonRpcResult.isError() -> Failure(
            McpError.Protocol(
                McpJson.convert<McpNodeType, ErrorMessage>(
                    jsonRpcResult.error!!
                )
            )
        )

        else -> Success(fn(McpJson.convert<McpNodeType, T>(jsonRpcResult.result!!)))
    }
}
