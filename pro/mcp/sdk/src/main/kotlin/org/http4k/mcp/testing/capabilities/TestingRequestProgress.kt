package org.http4k.mcp.testing.capabilities

import org.http4k.mcp.client.McpClient
import org.http4k.mcp.model.Progress
import org.http4k.mcp.protocol.messages.McpProgress
import org.http4k.mcp.testing.TestMcpSender
import org.http4k.mcp.testing.nextNotification

class TestingRequestProgress(sender: TestMcpSender) : McpClient.RequestProgress {

    init {
        sender.on(McpProgress) { event ->
            listOf(event).asSequence().nextNotification<McpProgress.Notification>(McpProgress)
                .also { n -> progress.forEach { it(Progress(n.progress, n.total, n.progressToken)) } }
        }
    }

    private val progress = mutableListOf<(Progress) -> Unit>()

    override fun onProgress(fn: (Progress) -> Unit) {
        progress += fn
    }
}
