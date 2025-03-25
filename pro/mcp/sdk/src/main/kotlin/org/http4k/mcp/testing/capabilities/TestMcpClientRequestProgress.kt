package org.http4k.mcp.testing.capabilities

import org.http4k.mcp.client.McpClient
import org.http4k.mcp.model.Progress
import org.http4k.mcp.protocol.messages.McpProgress
import org.http4k.mcp.testing.nextNotification
import org.http4k.testing.TestSseClient
import java.util.concurrent.atomic.AtomicReference

class TestMcpClientRequestProgress(private val client: AtomicReference<TestSseClient>) :
    McpClient.RequestProgress {

    private val progress = mutableListOf<(Progress) -> Unit>()

    override fun onProgress(fn: (Progress) -> Unit) {
        progress += fn
    }

    /**
     * Force a list changed notification to be received and process it
     */
    fun expectProgress() = client.nextNotification<McpProgress.Notification>(McpProgress)
        .also { n -> progress.forEach { it(Progress(n.progress, n.total, n.progressToken)) } }
}
