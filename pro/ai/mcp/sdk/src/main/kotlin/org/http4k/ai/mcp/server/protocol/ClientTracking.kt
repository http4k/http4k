/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

import org.http4k.ai.mcp.model.CompletionStatus
import org.http4k.ai.mcp.model.CompletionStatus.Finished
import org.http4k.ai.mcp.model.McpMessageId
import org.http4k.ai.mcp.protocol.messages.McpInitialize
import org.http4k.ai.mcp.util.McpNodeType
import java.util.concurrent.ConcurrentHashMap

class ClientTracking(initialize: McpInitialize.Request.Params) {
    val supportsSampling = initialize.capabilities.sampling != null
    val supportsRoots = initialize.capabilities.roots?.listChanged == true
    val supportsElicitation = initialize.capabilities.elicitation != null
    val supportsTasks = initialize.capabilities.tasks != null

    private val calls = ConcurrentHashMap<McpMessageId, (McpNodeType) -> CompletionStatus>()

    fun trackRequest(id: McpMessageId, callback: (McpNodeType) -> CompletionStatus) {
        calls[id] = callback
    }

    fun processResult(id: McpMessageId, result: McpNodeType) {
        val done = calls[id]?.let {
            synchronized(it) {
                it(result)
            }
        } ?: Finished
        if (done == Finished) calls.remove(id)
    }
}
