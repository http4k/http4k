package blaise

import blaise.HistoryItem.Ai
import blaise.HistoryItem.ToolApproved
import blaise.HistoryItem.ToolConsent
import blaise.HistoryItem.ToolDenied
import blaise.HistoryItem.User
import org.http4k.ai.llm.model.Content
import org.http4k.ai.llm.tools.ToolRequest
import org.http4k.ai.model.RequestId

interface History : Iterable<HistoryItem> {
    fun addUser(message: String): User
    fun addAi(contents: List<Content>): Ai
    fun addToolConsent(request: ToolRequest): ToolConsent
    fun addToolApproved(id: RequestId): ToolApproved
    fun addToolDenied(id: RequestId): ToolDenied

    companion object
}

