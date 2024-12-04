@file:OptIn(ExperimentalKotshiApi::class)

package org.http4k.connect.azure.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.azure.AzureAIMoshi.autoBody
import org.http4k.connect.azure.CompletionId
import org.http4k.connect.azure.ObjectType
import org.http4k.connect.azure.User
import org.http4k.connect.azure.action.Detail.auto
import org.http4k.connect.model.FinishReason
import org.http4k.connect.model.ModelName
import org.http4k.connect.model.Role
import org.http4k.connect.model.Timestamp
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.with
import se.ansman.kotshi.ExperimentalKotshiApi
import se.ansman.kotshi.JsonProperty
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.Polymorphic
import se.ansman.kotshi.PolymorphicLabel

@Http4kConnectAction
@JsonSerializable
data class ChatCompletion(
    val model: ModelName,
    val messages: List<Message>,
    val max_tokens: Int? = null,
    val temperature: Double = 1.0,
    val top_p: Double = 1.0,
    val seed: Int = 1,
    val stop: List<String>? = null,
    val presence_penalty: Double = 0.0,
    val frequency_penalty: Double = 0.0,
    val user: User? = null,
    override val stream: Boolean = false,
    val response_format: ResponseFormat? = null,
    val tools: List<Tool>? = null,
    val tool_choice: Any? = null,
    val n: Integer? = null,
) : ModelCompletion {
    override fun toRequest() = Request(Method.POST, "/chat/completions")
        .with(autoBody<ChatCompletion>().toLens() of this)

    constructor(model: ModelName, messages: List<Message>, max_tokens: Int = 16, stream: Boolean = true) : this(
        model,
        messages,
        max_tokens,
        stream = stream,
        tool_choice = null
    )

    constructor(model: ModelName, message: Message, max_tokens: Int = 16, stream: Boolean = true) : this(
        model,
        listOf(message),
        max_tokens,
        stream = stream,
        tool_choice = null
    )

    init {
        require(tools == null || tools.isNotEmpty()) { "Tools cannot be empty" }
    }

    override fun content() = messages
}


@JsonSerializable
@Polymorphic("type")
sealed class ResponseFormat {
    @JsonSerializable
    @PolymorphicLabel("json_object")
    data object Json : ResponseFormat()

    @JsonSerializable
    @PolymorphicLabel("url")
    data object Url : ResponseFormat()

    @JsonSerializable
    @PolymorphicLabel("json_schema")
    data class JsonSchema(val strict: Boolean, val json_schema: Map<String, Any>) : ResponseFormat()
}


@JsonSerializable
data class Message(
    val role: Role,
    val content: List<MessageContent>?,
    val name: User? = null,
    val tool_calls: List<ToolCall>? = null
) {
    companion object {
        fun User(content: String, name: User? = null) = User(listOf(MessageContent(ContentType.text, content)), name)
        fun User(content: List<MessageContent>, name: User? = null) = Message(Role.User, content, name, null)

        fun System(content: String, name: User? = null) =
            System(listOf(MessageContent(ContentType.text, content)), name)

        fun System(content: List<MessageContent>, name: User? = null) = Message(Role.System, content, name)

        fun Assistant(content: String, name: User? = null) =
            Assistant(listOf(MessageContent(ContentType.text, content)), name)

        fun Assistant(content: List<MessageContent>, name: User? = null) =
            Message(Role.Assistant, content, name)

        @JvmName("AssistantToolCalls")
        fun Assistant(tool_calls: List<ToolCall>, name: User? = null) =
            Message(Role.Assistant, null, name, tool_calls)
    }

}

@JsonSerializable
data class Function(val name: String)

@JsonSerializable
data class ToolChoice(val function: Function) {
    val type = "function"
}

@JsonSerializable
data class MessageContent(
    val type: ContentType,
    val text: String? = null,
    val image_url: ImageUrl? = null
)

@JsonSerializable
data class ImageUrl(val url: Uri, val detail: Detail = auto)

enum class Detail {
    low, high, auto
}

enum class ContentType {
    text, image_url
}

@JsonSerializable
data class Choice(
    val index: Int,
    @JsonProperty(name = "message")
    internal val msg: ChoiceDetail?,
    internal val delta: ChoiceDetail?,
    val finish_reason: FinishReason?,
) {
    val message get() = msg ?: delta
}

@JsonSerializable
data class ChoiceDetail(
    @JsonProperty(name = "role")
    internal val r: Role?,
    val content: String? = null,
    val tool_calls: List<ToolCall>? = null,
) {
    val role = r ?: Role.Assistant
}

@JsonSerializable
data class ToolCall(
    val id: String,
    val type: String,
    val function: FunctionCall,
    val index: Int? = null
)

@JsonSerializable
data class Tool(val function: FunctionSpec) {
    val type = "function"
}

@JsonSerializable
data class FunctionSpec(
    val name: String,
    val parameters: Any? = null, // JSON schema format
    val description: String? = null,
) {
    val type = "function"
}

@JsonSerializable
data class FunctionCall(
    val name: String,
    val arguments: String
)

@JsonSerializable
data class CompletionResponse(
    val id: CompletionId,
    val created: Timestamp,
    val model: ModelName,
    val choices: List<Choice>,
    @JsonProperty(name = "object")
    val objectType: ObjectType,
    val usage: Usage? = null,
)
