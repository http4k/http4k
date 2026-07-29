/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.model

import java.time.Instant

/**
 * The handle a server hands back in place of a result when work will outlive the request. This is the
 * handler-facing model: the terminal payloads (result/error) and any outstanding inputRequests belong to
 * the wire form served by tasks/get, not to the moment of creation.
 */
data class Task(
    val taskId: TaskId,
    val status: TaskStatus,
    val createdAt: Instant,
    val lastUpdatedAt: Instant,
    val statusMessage: String? = null,
    val ttlMs: TtlMs? = null,
    val pollIntervalMs: Long? = null,
)
