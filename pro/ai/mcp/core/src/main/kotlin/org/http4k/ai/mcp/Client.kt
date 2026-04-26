/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp

import org.http4k.ai.mcp.model.ElicitationId
import org.http4k.ai.mcp.model.LogLevel
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.model.ProgressToken
import org.http4k.ai.mcp.model.Task
import org.http4k.ai.mcp.model.TaskId
import java.time.Duration

interface Client {
    fun elicit(request: ElicitationRequest, fetchNextTimeout: Duration? = null): McpResult<ElicitationResponse>
    fun sample(request: SamplingRequest, fetchNextTimeout: Duration? = null): Sequence<McpResult<SamplingResponse>>
    fun progress(progressToken: ProgressToken, progress: Int, total: Double? = null, description: String? = null)
    fun log(data: Any, level: LogLevel, logger: String? = null)
    fun elicitationComplete(elicitationId: ElicitationId)
    fun updateTask(task: Task, meta: Meta = Meta.default)
    fun storeTaskResult(taskId: TaskId, result: Map<String, Any>)
    fun requestRoots(meta: Meta = Meta.default)

    companion object {
        object NoOp : Client {
            override fun elicit(request: ElicitationRequest, fetchNextTimeout: Duration?) = error("NoOp")
            override fun sample(request: SamplingRequest, fetchNextTimeout: Duration?) = error("NoOp")
            override fun progress(progressToken: ProgressToken, progress: Int, total: Double?, description: String?) = error("NoOp")
            override fun log(data: Any, level: LogLevel, logger: String?) = error("NoOp")
            override fun elicitationComplete(elicitationId: ElicitationId) = error("NoOp")
            override fun updateTask(task: Task, meta: Meta) = error("NoOp")
            override fun requestRoots(meta: Meta) = error("NoOp")
            override fun storeTaskResult(taskId: TaskId, result: Map<String, Any>) = error("NoOp")
        }
    }
}
