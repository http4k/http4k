/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp

import org.http4k.ai.mcp.model.LogLevel
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.model.ProgressToken
import org.http4k.ai.mcp.model.Task
import org.http4k.ai.mcp.model.TaskId

// ponytail: server→client push (elicit/sample/requestRoots/elicitationComplete) removed for the
// stateless model — re-added via MRTR in Stage 4. progress/log/task hooks kept (request-scoped).
interface Client {
    fun progress(progressToken: ProgressToken, progress: Int, total: Double? = null, description: String? = null)
    fun log(data: Any, level: LogLevel, logger: String? = null)
    fun updateTask(task: Task, meta: Meta = Meta.default)
    fun storeTaskResult(taskId: TaskId, result: Map<String, Any>)

    companion object {
        // ponytail: fire-and-forget notifications with no channel are silently dropped (not errors)
        // until progress/log/tasks are re-wired request-scoped (progress w/ transport, logging Stage 8,
        // tasks Stage 9).
        object NoOp : Client {
            override fun progress(progressToken: ProgressToken, progress: Int, total: Double?, description: String?) = Unit
            override fun log(data: Any, level: LogLevel, logger: String?) = Unit
            override fun updateTask(task: Task, meta: Meta) = Unit
            override fun storeTaskResult(taskId: TaskId, result: Map<String, Any>) = Unit
        }
    }
}
