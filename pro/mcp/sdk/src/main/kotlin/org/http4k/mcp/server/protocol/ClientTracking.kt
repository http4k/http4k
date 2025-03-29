package org.http4k.mcp.server.protocol

import org.http4k.format.MoshiNode
import org.http4k.jsonrpc.JsonRpcResult
import org.http4k.mcp.model.CompletionStatus
import org.http4k.mcp.model.McpMessageId
import org.http4k.mcp.protocol.messages.McpInitialize
import org.http4k.mcp.util.McpNodeType
import java.util.concurrent.ConcurrentHashMap

class ClientTracking(initialize: McpInitialize.Request) {
    val supportsSampling = initialize.capabilities.sampling != null
    val supportsRoots = initialize.capabilities.roots?.listChanged == true

    private val calls = ConcurrentHashMap<McpMessageId, (JsonRpcResult<McpNodeType>) -> CompletionStatus>()

    fun trackRequest(id: McpMessageId, callback: (JsonRpcResult<McpNodeType>) -> CompletionStatus) {
        calls[id] = callback
    }

    fun processResult(id: McpMessageId, result: JsonRpcResult<MoshiNode>) {
        val done = calls[id]?.invoke(result) ?: CompletionStatus.Finished
        if (done == CompletionStatus.Finished) calls.remove(id)
    }
}
