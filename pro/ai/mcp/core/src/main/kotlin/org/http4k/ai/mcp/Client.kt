package org.http4k.ai.mcp

import org.http4k.ai.mcp.model.LogLevel
import java.time.Duration

interface Client {
    fun elicit(request: ElicitationRequest, fetchNextTimeout: Duration? = null): McpResult<ElicitationResponse>
    fun sample(request: SamplingRequest, fetchNextTimeout: Duration? = null): Sequence<McpResult<SamplingResponse>>
    fun progress(progress: Int, total: Double? = null, description: String? = null)
    fun log(data: Any, level: LogLevel, logger: String? = null)

    companion object {
        object NoOp : Client {
            override fun elicit(request: ElicitationRequest, fetchNextTimeout: Duration?) = error("NoOp")
            override fun sample(request: SamplingRequest, fetchNextTimeout: Duration?) = error("NoOp")
            override fun progress(progress: Int, total: Double?, description: String?) {}
            override fun log(data: Any, level: LogLevel, logger: String?) {}
        }
    }
}
