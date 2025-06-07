package chatzilla

import chatzilla.HistoryContent.Ai
import chatzilla.HistoryContent.ToolApproved
import chatzilla.HistoryContent.ToolConsent
import chatzilla.HistoryContent.ToolDenied
import chatzilla.HistoryContent.User
import org.http4k.ai.llm.tools.ToolRequest
import org.http4k.ai.model.RequestId
import org.http4k.template.ViewModel

class ChatHistory(welcomeMessage: String) {
    private val _content = mutableListOf<HistoryContent>(Ai("0", welcomeMessage))

    fun addUser(message: String) = User(_content.size.toString(), message).also(_content::add)
    fun addAi(message: String) = Ai(_content.size.toString(), message).also(_content::add)
    fun addToolConsent(request: ToolRequest) = ToolConsent(request.id.value, request).also(_content::add)

    fun addToolApproved(id: RequestId): ToolApproved {
        val index = _content.indexOfFirst { it.id == id.value }
        val consent = _content[index] as ToolConsent
        return ToolApproved(id.value, consent.request).also { _content[index] = it }
    }

    fun addToolDenied(id: RequestId): ToolDenied {
        val index = _content.indexOfFirst { it.id == id.value }
        val consent = _content[index] as ToolConsent
        return ToolDenied(id.value, consent.request).also { _content[index] = it }
    }

    val content: Iterable<HistoryContent> get() = _content.toList()
}

sealed interface HistoryContent : ViewModel {
    val id: String

    data class User(override val id: String, val content: String) : HistoryContent
    data class Ai(override val id: String, val content: String) : HistoryContent
    data class ToolConsent(override val id: String, val request: ToolRequest) : HistoryContent
    data class ToolApproved(override val id: String, val request: ToolRequest) : HistoryContent
    data class ToolDenied(override val id: String, val request: ToolRequest) : HistoryContent
}
