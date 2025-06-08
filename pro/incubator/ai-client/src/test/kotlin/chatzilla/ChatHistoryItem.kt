package chatzilla

import org.http4k.ai.llm.model.Content
import org.http4k.ai.llm.model.Content.Text
import org.http4k.ai.llm.tools.ToolRequest
import org.http4k.template.ViewModel
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.Polymorphic
import se.ansman.kotshi.PolymorphicLabel

@JsonSerializable
@Polymorphic("type")
sealed class ChatHistoryItem : ViewModel {
    abstract val id: String

    @JsonSerializable
    @PolymorphicLabel("user")
    data class User(override val id: String, val text: String) : ChatHistoryItem()

    @JsonSerializable
    @PolymorphicLabel("ai")
    data class Ai(override val id: String, val contents: List<Content>) : ChatHistoryItem() {
        val text
            get() = contents.joinToString(separator = "\n") {
                when (it) {
                    is Text -> it.text
                    else -> ""
                }
            }
    }

    @JsonSerializable
    @PolymorphicLabel("toolConsent")
    data class ToolConsent(override val id: String, val request: ToolRequest) : ChatHistoryItem()

    @JsonSerializable
    @PolymorphicLabel("toolApproved")
    data class ToolApproved(override val id: String, val request: ToolRequest) : ChatHistoryItem()

    @JsonSerializable
    @PolymorphicLabel("toolDenied")
    data class ToolDenied(override val id: String, val request: ToolRequest) : ChatHistoryItem()
}
