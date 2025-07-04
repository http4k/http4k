package org.http4k.connect.anthropic.action

import org.http4k.ai.model.Role
import org.http4k.ai.model.ToolName
import org.http4k.connect.anthropic.SourceType
import org.http4k.connect.anthropic.ToolUseId
import org.http4k.connect.anthropic.UserId
import org.http4k.connect.model.Base64Blob
import org.http4k.connect.model.MimeType
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.Polymorphic
import se.ansman.kotshi.PolymorphicLabel

@JsonSerializable
data class Source(
    val data: Base64Blob,
    val media_type: MimeType,
    val type: SourceType = SourceType.base64
)

@JsonSerializable
@Polymorphic("type")
sealed class Content {
    @JsonSerializable
    @PolymorphicLabel("text")
    data class Text(val text: String) : Content()

    @JsonSerializable
    @PolymorphicLabel("image")
    data class Image(val source: Source) : Content()

    @JsonSerializable
    @PolymorphicLabel("tool_use")
    data class ToolUse(val name: ToolName, val id: ToolUseId, val input: Map<String, Any>) : Content()

    @JsonSerializable
    @PolymorphicLabel("tool_result")
    data class ToolResult(val tool_use_id: ToolUseId, val content: Any) : Content()
}

@JsonSerializable
data class Message(val role: Role, val content: List<Content>) {
    companion object {
        fun User(content: Content) = Message(Role.User, listOf(content))
        fun User(content: List<Content>) = Message(Role.User, content)
        fun System(content: Content) = Message(Role.System, listOf(content))
        fun System(content: List<Content>) = Message(Role.System, content)
        fun Assistant(content: Content) = Message(Role.Assistant, listOf(content))
        fun Assistant(content: List<Content>) = Message(Role.Assistant, content)
        fun Tool(content: Content) = Message(Role.Tool, listOf(content))
        fun Tool(content: List<Content>) = Message(Role.Tool, content)
    }
}

@JsonSerializable
data class Tool(
    val name: ToolName,
    val description: String,
    val input_schema: Map<String, Any>,
    val type: ToolType? = null,
    val cache_control: CacheControl? = null
)

enum class ToolType {
    ephemeral
}

enum class CacheControl {
    ephemeral
}

@JsonSerializable
data class Metadata(val user_id: UserId?)

@JsonSerializable
data class Usage(
    val input_tokens: Int? = null,
    val cache_creation_input_tokens: Int? = null,
    val cache_read_input_tokens: Int? = null,
    val output_tokens: Int? = null
)
