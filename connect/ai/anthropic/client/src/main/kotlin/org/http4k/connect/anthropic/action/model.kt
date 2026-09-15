package org.http4k.connect.anthropic.action

import org.http4k.ai.model.Role
import org.http4k.ai.model.ToolName
import org.http4k.connect.anthropic.FileId
import org.http4k.connect.anthropic.ToolType
import org.http4k.connect.anthropic.ToolUseId
import org.http4k.connect.anthropic.UserId
import org.http4k.core.Uri
import se.ansman.kotshi.JsonDefaultValue
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.Polymorphic
import se.ansman.kotshi.PolymorphicLabel
import java.time.Instant

@JsonSerializable
@Polymorphic("type")
sealed class Content {
    @JsonSerializable
    @PolymorphicLabel("text")
    data class Text(
        val text: String,
        val cache_control: CacheControl? = null,
        val citations: List<Citation>? = null
    ) : Content()

    @JsonSerializable
    @PolymorphicLabel("image")
    data class Image(
        val source: Source,
        val transformations: ImageTransformations? = null,
        val cache_control: CacheControl? = null
    ) : Content()

    @JsonSerializable
    @PolymorphicLabel("document")
    data class Document(
        val source: DocumentSource,
        val title: String? = null,
        val context: String? = null,
        val citations: Citations? = null,
        val cache_control: CacheControl? = null
    ) : Content()

    @JsonSerializable
    @PolymorphicLabel("search_result")
    data class SearchResult(
        val source: Uri,
        val title: String,
        val content: List<Text>,
        val citations: Citations? = null,
        val cache_control: CacheControl? = null
    ) : Content()

    @JsonSerializable
    @PolymorphicLabel("container_upload")
    data class ContainerUpload(val file_id: FileId, val cache_control: CacheControl? = null) : Content()

    @JsonSerializable
    @PolymorphicLabel("thinking")
    data class Thinking(val thinking: String, val signature: String) : Content()

    @JsonSerializable
    @PolymorphicLabel("redacted_thinking")
    data class RedactedThinking(val `data`: String) : Content()

    @JsonSerializable
    @PolymorphicLabel("tool_use")
    data class ToolUse(
        val name: ToolName,
        val id: ToolUseId,
        val input: Map<String, Any>,
        val caller: ToolCaller? = null,
        val cache_control: CacheControl? = null
    ) : Content()

    @JsonSerializable
    @PolymorphicLabel("tool_result")
    data class ToolResult(
        val tool_use_id: ToolUseId,
        val content: List<Content>,
        val is_error: Boolean? = null,
        val cache_control: CacheControl? = null
    ) : Content()

    @JsonSerializable
    @PolymorphicLabel("server_tool_use")
    data class ServerToolUse(
        val id: ToolUseId,
        val name: ToolName,
        val input: Map<String, Any>,
        val cache_control: CacheControl? = null
    ) : Content()

    @JsonSerializable
    @PolymorphicLabel("web_search_tool_result")
    data class WebSearchToolResult(val tool_use_id: ToolUseId, val content: WebSearchContent) : Content()

    @JsonSerializable
    @PolymorphicLabel("web_fetch_tool_result")
    data class WebFetchToolResult(val tool_use_id: ToolUseId, val content: WebFetchContent) : Content()

    @JsonSerializable
    @PolymorphicLabel("code_execution_tool_result")
    data class CodeExecutionToolResult(
        val tool_use_id: ToolUseId,
        val content: CodeExecutionContent
    ) : Content()

    @JsonSerializable
    @PolymorphicLabel("bash_code_execution_tool_result")
    data class BashCodeExecutionToolResult(
        val tool_use_id: ToolUseId,
        val content: BashCodeExecutionContent
    ) : Content()

    @JsonSerializable
    @PolymorphicLabel("text_editor_code_execution_tool_result")
    data class TextEditorCodeExecutionToolResult(
        val tool_use_id: ToolUseId,
        val content: TextEditorCodeExecutionContent
    ) : Content()

    @JsonSerializable
    @PolymorphicLabel("tool_search_tool_result")
    data class ToolSearchToolResult(val tool_use_id: ToolUseId, val content: ToolSearchContent) : Content()

    @JsonSerializable
    @JsonDefaultValue
    data class Unknown(val raw: String) : Content()
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

sealed class Tool {

    @JsonSerializable
    data class User(
        val name: ToolName,
        val input_schema: Map<String, Any>,
        val description: String? = null,
        val strict: Boolean? = null,
        val defer_loading: Boolean? = null,
        val allowed_callers: List<Caller>? = null,
        val eager_input_streaming: Boolean? = null,
        val input_examples: List<Map<String, Any>>? = null,
        val cache_control: CacheControl? = null,
        val type: String = "custom"
    ) : Tool()

    @JsonSerializable
    data class BuiltIn(
        val type: ToolType,
        val name: ToolName? = null,
        val max_uses: Int? = null,
        val max_characters: Int? = null,
        val max_content_tokens: Int? = null,
        val allowed_domains: List<String>? = null,
        val blocked_domains: List<String>? = null,
        val user_location: UserLocation? = null,
        val response_inclusion: String? = null,
        val citations: Citations? = null,
        val use_cache: Boolean? = null,
        val configs: List<ToolConfig>? = null,
        val strict: Boolean? = null,
        val defer_loading: Boolean? = null,
        val allowed_callers: List<Caller>? = null,
        val cache_control: CacheControl? = null
    ) : Tool()

    companion object
}

@JsonSerializable
data class ToolCaller(val type: Caller, val tool_id: String? = null)

enum class Caller {
    direct, code_execution_20250825, code_execution_20260120, code_execution_20260521
}

@JsonSerializable
data class UserLocation(
    val city: String? = null,
    val region: String? = null,
    val country: String? = null,
    val timezone: String? = null,
    val type: String = "approximate"
)

@JsonSerializable
data class Citations(val enabled: Boolean)

@JsonSerializable
data class ToolConfig(val name: String, val enabled: Boolean? = null, val defer_loading: Boolean? = null)

@JsonSerializable
data class CacheControl(val ttl: CacheTtl? = null, val type: String = "ephemeral")

enum class CacheTtl {
    `5m`, `1h`
}

@JsonSerializable
data class OutputConfig(val effort: Effort? = null, val format: OutputFormat? = null)

enum class Effort {
    low, medium, high, xhigh, max
}

@JsonSerializable
data class OutputFormat(val schema: Map<String, Any>, val type: String = "json_schema")

@JsonSerializable
data class RefusalDetails(
    val category: RefusalCategory? = null,
    val explanation: String? = null,
    val type: String = "refusal"
)

enum class RefusalCategory {
    cyber, bio, frontier_llm, reasoning_extraction, general_harms,

    @JsonDefaultValue
    unknown
}

@JsonSerializable
data class Container(val id: ContainerId, val expires_at: Instant? = null)

@JsonSerializable
data class CacheCreation(
    val ephemeral_5m_input_tokens: Int? = null,
    val ephemeral_1h_input_tokens: Int? = null
)

@JsonSerializable
data class OutputTokensDetails(val thinking_tokens: Int? = null)

@JsonSerializable
data class ServerToolUsage(
    val web_search_requests: Int? = null,
    val web_fetch_requests: Int? = null
)

@JsonSerializable
data class Metadata(val user_id: UserId?)

@JsonSerializable
data class Usage(
    val input_tokens: Int? = null,
    val cache_creation_input_tokens: Int? = null,
    val cache_read_input_tokens: Int? = null,
    val output_tokens: Int? = null,
    val cache_creation: CacheCreation? = null,
    val output_tokens_details: OutputTokensDetails? = null,
    val server_tool_use: ServerToolUsage? = null,
    val service_tier: UsageServiceTier? = null,
    val inference_geo: InferenceGeo? = null
)
