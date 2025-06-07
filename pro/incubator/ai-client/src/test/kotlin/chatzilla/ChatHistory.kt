package chatzilla

import org.http4k.template.ViewModel

class ChatHistory(welcomeMessage: String) {
    private val _content = mutableListOf<HistoryContent>(
        HistoryContent.Ai(-1, welcomeMessage)
    )

    fun addUser(message: String) = HistoryContent.User(_content.size, message).also(_content::add)
    fun addAi(message: String) = HistoryContent.Ai(_content.size, message).also(_content::add)
    fun addToolConsent(message: String) = HistoryContent.ToolConsent(_content.size, message).also(_content::add)
    fun addToolApproved(message: String) = HistoryContent.ToolApproved(_content.size, message).also(_content::add)
    fun addToolDenied(message: String) = HistoryContent.ToolDenied(_content.size, message).also(_content::add)

    val content: Iterable<HistoryContent> get() = _content.toList()
}

sealed interface HistoryContent : ViewModel {
    val id: Int
    val content: String

    data class User(override val id: Int, override val content: String) : HistoryContent
    data class Ai(override val id: Int, override val content: String) : HistoryContent
    data class ToolConsent(override val id: Int, override val content: String) : HistoryContent
    data class ToolApproved(override val id: Int, override val content: String) : HistoryContent
    data class ToolDenied(override val id: Int, override val content: String) : HistoryContent
}
