@file:OptIn(ExperimentalKotshiApi::class)

package org.http4k.connect.openai.action

import dev.forkhandles.result4k.Result
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.RemoteFailure
import org.http4k.connect.model.FinishReason
import org.http4k.connect.model.ModelName
import org.http4k.connect.model.Role
import org.http4k.connect.model.Timestamp
import org.http4k.connect.openai.CompletionId
import org.http4k.connect.openai.ObjectType
import org.http4k.connect.openai.OpenAIAction
import org.http4k.connect.openai.OpenAIMoshi
import org.http4k.connect.openai.OpenAIMoshi.autoBody
import org.http4k.connect.openai.TokenId
import org.http4k.connect.openai.User
import org.http4k.connect.openai.action.Detail.auto
import org.http4k.connect.util.toCompletionSequence
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
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
    val n: Int = 1,
    val stop: List<String>? = null,
    val presence_penalty: Double = 0.0,
    val frequency_penalty: Double = 0.0,
    val logit_bias: Map<TokenId, Double>? = null,
    val user: User? = null,
    val stream: Boolean = false,
    val response_format: ResponseFormat? = null,
    val tools: List<Tool>? = null,
    val tool_choice: Any? = null,
    val parallel_tool_calls: Boolean? = null,
    val service_tier: String? = null,
    val seed: Int? = null,
    val stream_options: StreamOptions? = null
) : OpenAIAction<Sequence<CompletionResponse>> {
    constructor(model: ModelName, messages: List<Message>, max_tokens: Int = 16, stream: Boolean = true) : this(
        model,
        messages,
        max_tokens = max_tokens,
        temperature = 1.0,
        top_p = 1.0,
        n = 1,
        stop = null,
        presence_penalty = 0.0,
        frequency_penalty = 0.0,
        logit_bias = null,
        user = null,
        stream = stream
    )

    constructor(model: ModelName, message: Message, max_tokens: Int = 16, stream: Boolean = true) : this(
        model,
        listOf(message),
        max_tokens = max_tokens,
        temperature = 1.0,
        top_p = 1.0,
        n = 1,
        stop = null,
        presence_penalty = 0.0,
        frequency_penalty = 0.0,
        logit_bias = null,
        user = null,
        stream = stream
    )

    init {
        require(tools == null || tools.isNotEmpty()) { "Tools cannot be empty" }
    }

    override fun toRequest() = Request(POST, "/v1/chat/completions")
        .with(autoBody<ChatCompletion>().toLens() of this)

    override fun toResult(response: Response): Result<Sequence<CompletionResponse>, RemoteFailure> =
        toCompletionSequence(response, OpenAIMoshi, "data: ", "[DONE]")
}

@JsonSerializable
data class StreamOptions(val include_usage: Boolean? = null)

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
    val role: Role?,
    val content: List<MessageContent>? = null,
    val name: User? = null,
    val refusal: String? = null,
    val tool_calls: List<ToolCall>? = null
) {
    companion object {
        fun User(content: String, name: User? = null) = User(listOf(MessageContent(ContentType.text, content)), name)
        fun User(content: List<MessageContent>, name: User? = null) = Message(Role.User, content, name, null)

        fun System(content: String, name: User? = null) =
            System(listOf(MessageContent(ContentType.text, content)), name)

        fun System(content: List<MessageContent>, name: User? = null) = Message(Role.System, content, name)

        fun Assistant(content: String, name: User? = null, refusal: String? = null) =
            Assistant(listOf(MessageContent(ContentType.text, content)), name, refusal)

        fun Assistant(content: List<MessageContent>, name: User? = null, refusal: String? = null) =
            Message(Role.Assistant, content, name, refusal)

        @JvmName("AssistantToolCalls")
        fun Assistant(tool_calls: List<ToolCall>, name: User? = null, refusal: String? = null) =
            Message(Role.Assistant, null, name, refusal, tool_calls)
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
    val finish_reason: FinishReason?
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
    val system_fingerprint: String? = null,
    val service_tier: String? = null
)
