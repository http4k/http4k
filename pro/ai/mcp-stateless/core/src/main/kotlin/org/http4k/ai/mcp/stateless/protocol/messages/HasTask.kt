/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.protocol.messages

import org.http4k.ai.mcp.stateless.model.TaskId
import org.http4k.ai.mcp.stateless.model.TaskStatus
import org.http4k.ai.mcp.stateless.model.TtlMs
import java.time.Instant

interface HasTask {
    val taskId: TaskId?
    val status: TaskStatus?
    val statusMessage: String?
    val createdAt: Instant?
    val lastUpdatedAt: Instant?
    val ttlMs: TtlMs?
    val pollIntervalMs: Long?
}
