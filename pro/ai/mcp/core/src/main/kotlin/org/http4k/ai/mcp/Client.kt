package org.http4k.ai.mcp

import java.time.Duration

interface Client {
    fun elicit(request: ElicitationRequest, fetchNextTimeout: Duration? = null): McpResult<ElicitationResponse>
    fun sample(request: SamplingRequest, fetchNextTimeout: Duration? = null): Sequence<McpResult<SamplingResponse>>
    fun progress(progress: Int, total: Double? = null, description: String? = null)

    companion object {
        object NoOp : Client {
            override fun elicit(request: ElicitationRequest, fetchNextTimeout: Duration?) = error("NoOp")
            override fun sample(request: SamplingRequest, fetchNextTimeout: Duration?) = error("NoOp")
            override fun progress(progress: Int, total: Double?, description: String?) = error("NoOp")
        }
    }
}

