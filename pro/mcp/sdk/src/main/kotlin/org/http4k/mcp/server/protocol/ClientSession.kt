package org.http4k.mcp.server.protocol

import org.http4k.format.MoshiNode
import org.http4k.jsonrpc.JsonRpcResult
import org.http4k.mcp.model.CompletionStatus
import org.http4k.mcp.model.CompletionStatus.Finished
import org.http4k.mcp.model.RequestId
import org.http4k.mcp.protocol.ClientCapabilities
import org.http4k.mcp.protocol.VersionedMcpEntity
import org.http4k.mcp.util.McpNodeType
import java.util.concurrent.ConcurrentHashMap

class ClientSession(client: VersionedMcpEntity, val capabilities: ClientCapabilities) {
    private val calls = ConcurrentHashMap<RequestId, (JsonRpcResult<McpNodeType>) -> CompletionStatus>()

    fun addCall(id: RequestId, callback: (JsonRpcResult<McpNodeType>) -> CompletionStatus) {
        calls[id] = callback
    }

    fun processResult(id: RequestId, result: JsonRpcResult<MoshiNode>) {
        val done = calls[id]?.invoke(result) ?: Finished
        if (done == Finished) calls.remove(id)
    }

    val entity = client.name
}
