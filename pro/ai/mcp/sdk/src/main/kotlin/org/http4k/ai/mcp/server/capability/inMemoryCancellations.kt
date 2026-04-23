/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.capability

import org.http4k.ai.mcp.protocol.messages.McpCancelled
import org.http4k.ai.mcp.server.protocol.CancellationCallback
import org.http4k.ai.mcp.server.protocol.Cancellations
import java.util.concurrent.CopyOnWriteArrayList

fun cancellations() = object : Cancellations {
    private val callbacks = CopyOnWriteArrayList<CancellationCallback>()

    override fun onCancel(callback: CancellationCallback) {
        callbacks += callback
    }

    override fun cancel(cancellation: McpCancelled.Notification.Params) {
        callbacks.forEach { it(cancellation.requestId, cancellation.reason, cancellation._meta) }
    }
}
