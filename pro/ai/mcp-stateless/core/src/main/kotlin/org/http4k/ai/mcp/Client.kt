/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp

import org.http4k.ai.mcp.model.LogLevel
import org.http4k.ai.mcp.model.ProgressToken

// ponytail: server→client push (elicit/sample/requestRoots) removed for the stateless model — re-added
// via MRTR in Stage 4. progress/log kept (request-scoped notifications) but NoOp for now — re-wire
// request-scoped (progress w/ transport, logging Stage 8). Tasks deleted → re-add as extension in Stage 9.
interface Client {
    fun progress(progressToken: ProgressToken, progress: Int, total: Double? = null, description: String? = null)
    fun log(data: Any, level: LogLevel, logger: String? = null)

    companion object {
        object NoOp : Client {
            override fun progress(progressToken: ProgressToken, progress: Int, total: Double?, description: String?) = Unit
            override fun log(data: Any, level: LogLevel, logger: String?) = Unit
        }
    }
}
