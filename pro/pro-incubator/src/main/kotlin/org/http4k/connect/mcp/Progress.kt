package org.http4k.connect.mcp

import org.http4k.connect.mcp.McpRpcMethod.Companion.of

object Progress {

    data class Notification(val progress: Int, val total: Double?) : ServerNotification {
        override val method = of("notifications/progress")
    }
}

