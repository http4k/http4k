package org.http4k.mcp.capability

import org.http4k.mcp.OutgoingSamplingHandler
import org.http4k.mcp.SampleResponse
import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.protocol.messages.McpSampling

class OutgoingSamplingCapability(
    private val entity: McpEntity, private val handler: OutgoingSamplingHandler
) : ServerCapability {

    fun toEntity() = entity

    fun process(response: McpSampling.Response) = with(response) {
        handler(SampleResponse(model, stopReason, role, content))
    }
}
