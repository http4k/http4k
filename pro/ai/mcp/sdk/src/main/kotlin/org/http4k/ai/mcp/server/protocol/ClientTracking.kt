package org.http4k.ai.mcp.server.protocol

import org.http4k.ai.mcp.model.CompletionStatus
import org.http4k.ai.mcp.model.CompletionStatus.Finished
import org.http4k.ai.mcp.model.McpMessageId
import org.http4k.ai.mcp.protocol.messages.McpInitialize
import org.http4k.ai.mcp.util.McpNodeType
import org.http4k.format.MoshiNode
import org.http4k.jsonrpc.JsonRpcResult
import java.util.concurrent.ConcurrentHashMap

class ClientTracking(initialize: McpInitialize.Request) {
    val supportsSampling = initialize.capabilities.sampling != null
    val supportsRoots = initialize.capabilities.roots?.listChanged == true
    val supportsElicitation = initialize.capabilities.elicitation != null
    val supportsTasks = initialize.capabilities.tasks != null

    private val calls = ConcurrentHashMap<McpMessageId, (JsonRpcResult<McpNodeType>) -> CompletionStatus>()

    fun trackRequest(id: McpMessageId, callback: (JsonRpcResult<McpNodeType>) -> CompletionStatus) {
        calls[id] = callback
    }

    fun processResult(id: McpMessageId, result: JsonRpcResult<MoshiNode>) {
        val done = calls[id]?.let {
            synchronized(it) {
                it(result)
            }
        } ?: Finished
        if (done == Finished) calls.remove(id)
    }
}
