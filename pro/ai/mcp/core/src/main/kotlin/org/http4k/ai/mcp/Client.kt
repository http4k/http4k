package org.http4k.ai.mcp

import org.http4k.ai.mcp.model.ElicitationId
import org.http4k.ai.mcp.model.LogLevel
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.model.Task
import org.http4k.ai.mcp.model.TaskId
import java.time.Duration

interface Client {
    fun elicit(request: ElicitationRequest, fetchNextTimeout: Duration? = null): McpResult<ElicitationResponse>
    fun sample(request: SamplingRequest, fetchNextTimeout: Duration? = null): Sequence<McpResult<SamplingResponse>>
    fun progress(progress: Int, total: Double? = null, description: String? = null)
    fun log(data: Any, level: LogLevel, logger: String? = null)
    fun elicitationComplete(elicitationId: ElicitationId)
    fun updateTask(task: Task, meta: Meta = Meta.default, timeout: Duration? = null)
    fun storeTaskResult(taskId: TaskId, result: Map<String, Any>)

    companion object {
        object NoOp : Client {
            override fun elicit(request: ElicitationRequest, fetchNextTimeout: Duration?) = error("NoOp")
            override fun sample(request: SamplingRequest, fetchNextTimeout: Duration?) = error("NoOp")
            override fun progress(progress: Int, total: Double?, description: String?) = error("NoOp")
            override fun log(data: Any, level: LogLevel, logger: String?) = error("NoOp")
            override fun elicitationComplete(elicitationId: ElicitationId) = error("NoOp")
            override fun updateTask(task: Task, meta: Meta, timeout: Duration?) = error("NoOp")
            override fun storeTaskResult(taskId: TaskId, result: Map<String, Any>) = error("NoOp")
        }
    }
}
