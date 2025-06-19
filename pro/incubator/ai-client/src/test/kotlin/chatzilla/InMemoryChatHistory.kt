package chatzilla

import chatzilla.ChatHistoryItem.*
import org.http4k.ai.llm.model.Content
import org.http4k.ai.llm.tools.ToolRequest
import org.http4k.ai.model.RequestId

fun ChatHistory.Companion.InMemory() = object : ChatHistory {
    private val _content = mutableListOf<ChatHistoryItem>()

    override fun addUser(message: String) = User(_content.size.toString(), message).also(_content::add)
    override fun addAi(contents: List<Content>) = Ai(_content.size.toString(), contents).also(_content::add)
    override fun addToolConsent(request: ToolRequest) = ToolConsent(request.id.value, request).also(_content::add)

    override fun addToolApproved(id: RequestId): ToolApproved {
        val index = _content.indexOfFirst { it.id == id.value }
        val consent = _content[index] as ToolConsent
        return ToolApproved(id.value, consent.request).also { _content[index] = it }
    }

    override fun addToolDenied(id: RequestId): ToolDenied {
        val index = _content.indexOfFirst { it.id == id.value }
        val consent = _content[index] as ToolConsent
        return ToolDenied(id.value, consent.request).also { _content[index] = it }
    }

    override fun iterator() = _content.iterator()
}
