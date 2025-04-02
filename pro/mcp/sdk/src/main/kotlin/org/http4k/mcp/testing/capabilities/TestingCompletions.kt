package org.http4k.mcp.testing.capabilities

import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.map
import org.http4k.mcp.CompletionRequest
import org.http4k.mcp.CompletionResponse
import org.http4k.mcp.client.McpClient
import org.http4k.mcp.client.McpError
import org.http4k.mcp.model.Reference
import org.http4k.mcp.protocol.messages.McpCompletion
import org.http4k.mcp.protocol.messages.McpRpc
import org.http4k.mcp.testing.ResponsesToId
import org.http4k.mcp.testing.TestMcpSender
import org.http4k.mcp.testing.nextEvent
import org.http4k.mcp.util.McpJson
import org.http4k.sse.SseMessage
import java.time.Duration
//
//class InterceptOutbound {
//    private val outbound: (SseMessage.Event) -> Unit,
//
//    fun filterOut(responsesToId: ResponsesToId, mpcRpc: McpRpc) = responsesToId.events
//        .filter {
//            when {
//                it.isFor(mpcRpc) || it.isResult() -> true
//                else -> {
//                    this.outbound(it)
//                    false
//                }
//            }
//        }
//
//    private fun SseMessage.Event.isResult() = McpJson.fields(McpJson.parse(data)).toMap().containsKey("result")
//
//    private fun SseMessage.Event.isFor(rpc: McpRpc) =
//        McpJson.fields(McpJson.parse(data)).toMap()["method"]?.toString() == rpc.Method.value
//
//}

class TestingCompletions(
    private val sender: TestMcpSender,
) : McpClient.Completions {
    override fun complete(
        ref: Reference,
        request: CompletionRequest,
        overrideDefaultTimeout: Duration?
    ): Result<CompletionResponse, McpError> =
        sender(McpCompletion, McpCompletion.Request(ref, request.argument, request.meta))
            .nextEvent<McpCompletion.Response, CompletionResponse>(
                { CompletionResponse(completion.values, completion.total, completion.hasMore) }
            ).map { it.second }

}
