package org.http4k.ai.mcp.testing.capabilities

import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.model.Progress
import org.http4k.ai.mcp.protocol.messages.McpProgress
import org.http4k.ai.mcp.testing.TestMcpSender
import org.http4k.ai.mcp.testing.nextNotification

class TestingRequestProgress(sender: TestMcpSender) : McpClient.RequestProgress {

    init {
        sender.on(McpProgress) { event ->
            listOf(event).asSequence().nextNotification<McpProgress.Notification>(McpProgress)
                .also { n -> progress.forEach { it(Progress(n.progressToken, n.progress, n.total, n.description)) } }
        }
    }

    private val progress = mutableListOf<(Progress) -> Unit>()

    override fun onProgress(fn: (Progress) -> Unit) {
        progress += fn
    }
}
