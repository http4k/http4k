package chatzilla

import chatzilla.ChatHistoryItem.Ai
import chatzilla.ChatHistoryItem.ToolApproved
import chatzilla.ChatHistoryItem.ToolConsent
import chatzilla.ChatHistoryItem.ToolDenied
import chatzilla.ChatHistoryItem.User
import org.http4k.ai.llm.model.Content
import org.http4k.ai.llm.tools.ToolRequest
import org.http4k.ai.model.RequestId

interface ChatHistory : Iterable<ChatHistoryItem> {
    fun addUser(message: String): User
    fun addAi(contents: List<Content>): Ai
    fun addToolConsent(request: ToolRequest): ToolConsent
    fun addToolApproved(id: RequestId): ToolApproved
    fun addToolDenied(id: RequestId): ToolDenied

    companion object
}

