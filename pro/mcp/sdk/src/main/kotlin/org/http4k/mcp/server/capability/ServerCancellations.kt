package org.http4k.mcp.server.capability

import org.http4k.mcp.protocol.messages.McpCancelled
import org.http4k.mcp.server.protocol.CancellationCallback
import org.http4k.mcp.server.protocol.Cancellations
import java.util.concurrent.CopyOnWriteArrayList

class ServerCancellations : Cancellations {
    private val callbacks = CopyOnWriteArrayList<CancellationCallback>()

    override fun onCancel(callback: CancellationCallback) {
        callbacks += callback
    }

    override fun cancel(cancellation: McpCancelled.Notification) {
        callbacks.forEach { it(cancellation.requestId, cancellation.reason, cancellation._meta) }
    }
}
