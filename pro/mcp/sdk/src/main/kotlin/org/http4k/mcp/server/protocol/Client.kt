package org.http4k.mcp.server.protocol

import org.http4k.mcp.SamplingRequest
import org.http4k.mcp.SamplingResponse
import org.http4k.mcp.client.McpResult
import org.http4k.mcp.model.Progress
import java.time.Duration

interface Client {
    fun sample(request: SamplingRequest, fetchNextTimeout: Duration? = null): Sequence<McpResult<SamplingResponse>>
    fun report(req: Progress)

    companion object {
        object NoOp : Client {
            override fun sample(request: SamplingRequest, fetchNextTimeout: Duration?) = error("NoOp")
            override fun report(req: Progress) = error("NoOp")
        }
    }
}
