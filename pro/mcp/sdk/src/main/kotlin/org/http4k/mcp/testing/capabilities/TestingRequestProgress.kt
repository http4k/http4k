package org.http4k.mcp.testing.capabilities

import org.http4k.mcp.client.McpClient
import org.http4k.mcp.model.Progress
import org.http4k.mcp.protocol.messages.McpProgress
import org.http4k.mcp.testing.TestMcpSender
import org.http4k.mcp.testing.nextNotification

class TestingRequestProgress(private val sender: TestMcpSender) : McpClient.RequestProgress {

    private val progress = mutableListOf<(Progress) -> Unit>()

    override fun onProgress(fn: (Progress) -> Unit) {
        progress += fn
    }

    /**
     * Force a progress notification to be received and process it
     */
    fun expectProgress() = sender.stream().nextNotification<McpProgress.Notification>(McpProgress)
        .also { n -> progress.forEach { it(Progress(n.progress, n.total, n.progressToken)) } }
}
