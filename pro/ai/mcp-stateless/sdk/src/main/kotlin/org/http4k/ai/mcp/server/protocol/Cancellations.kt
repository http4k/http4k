/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

import org.http4k.ai.mcp.model.McpMessageId
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.protocol.messages.McpCancelled

/**
 * Handles Cancellation requests
 */
interface Cancellations {
    fun onCancel(callback: CancellationCallback)
    fun cancel(cancellation: McpCancelled.Notification.Params)
}

fun interface CancellationCallback {
    operator fun invoke(id: McpMessageId, reason: String?, meta: Meta)
}
