package org.http4k.mcp.server.capability

import org.http4k.mcp.OutgoingSamplingHandler
import org.http4k.mcp.SamplingResponse
import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.protocol.messages.McpSampling

class OutgoingSamplingCapability(
    private val entity: McpEntity, private val handler: OutgoingSamplingHandler
) : ServerCapability {

    fun toEntity() = entity

    fun process(response: McpSampling.Response) = with(response) {
        handler(SamplingResponse(model, role, content, stopReason))
    }
}
