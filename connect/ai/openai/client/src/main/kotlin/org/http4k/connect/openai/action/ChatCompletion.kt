@file:OptIn(ExperimentalKotshiApi::class)

package org.http4k.connect.openai.action

import dev.forkhandles.result4k.Result
import org.http4k.ai.model.MaxTokens
import org.http4k.ai.model.ModelName
import org.http4k.ai.model.ResponseId
import org.http4k.ai.model.Role
import org.http4k.ai.model.StopReason
import org.http4k.ai.model.Temperature
import org.http4k.ai.util.toCompletionSequence
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.RemoteFailure
import org.http4k.connect.model.Timestamp
import org.http4k.connect.openai.ObjectType
import org.http4k.connect.openai.OpenAIAction
import org.http4k.connect.openai.OpenAIMoshi
import org.http4k.connect.openai.OpenAIMoshi.autoBody
import org.http4k.connect.openai.TokenId
import org.http4k.connect.openai.User
import org.http4k.connect.openai.action.Detail.auto
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
    val max_tokens: MaxTokens? = null,
    val temperature: Temperature = Temperature.ONE,
    val top_p: Double? = null,
    val n: Int = 1,
    val stop: List<String>? = null,
    val presence_penalty: Double? = null,
    val frequency_penalty: Double? = null,
    val logit_bias: Map<TokenId, Double>? = null,
    val user: User? = null,
    val stream: Boolean = false,
    val response_format: ResponseFormat? = null,
    val tools: List<Tool>? = null,
    val tool_choice: Any? = null,
    val parallel_tool_calls: Boolean? = null,
    val service_tier: String? = null,
    val seed: Int? = null,
    val stream_options: StreamOptions? = null,
    val logprobs: Boolean? = null,
    val top_logprobs: Int? = null
) : OpenAIAction<Sequence<CompletionResponse>> {
    @Deprecated("Retained for binary compatibility", level = DeprecationLevel.HIDDEN)
    constructor(
        model: ModelName,
        messages: List<Message>,
        max_tokens: MaxTokens? = null,
        temperature: Temperature = Temperature.ONE,
        top_p: Double? = null,
        n: Int = 1,
        stop: List<String>? = null,
        presence_penalty: Double? = null,
        frequency_penalty: Double? = null,
        logit_bias: Map<TokenId, Double>? = null,
        user: User? = null,
        stream: Boolean = false,
        response_format: ResponseFormat? = null,
        tools: List<Tool>? = null,
        tool_choice: Any? = null,
        parallel_tool_calls: Boolean? = null,
        service_tier: String? = null,
        seed: Int? = null,
        stream_options: StreamOptions? = null
    ) : this(
        model = model,
        messages = messages,
        max_tokens = max_tokens,
        temperature = temperature,
        top_p = top_p,
        n = n,
        stop = stop,
        presence_penalty = presence_penalty,
        frequency_penalty = frequency_penalty,
        logit_bias = logit_bias,
        user = user,
        stream = stream,
        response_format = response_format,
        tools = tools,
        tool_choice = tool_choice,
        parallel_tool_calls = parallel_tool_calls,
        service_tier = service_tier,
        seed = seed,
        stream_options = stream_options,
        logprobs = null,
        top_logprobs = null
    )

    constructor(model: ModelName, messages: List<Message>, max_tokens: MaxTokens, stream: Boolean = true) : this(
        model = model,
        messages = messages,
        max_tokens = max_tokens,
        temperature = Temperature.ONE,
        top_p = 1.0,
        n = 1,
        stop = null,
        presence_penalty = 0.0,
        frequency_penalty = 0.0,
        logit_bias = null,
        user = null,
        stream = stream
    )

    constructor(model: ModelName, message: Message, max_tokens: MaxTokens, stream: Boolean = true) : this(
        model = model,
        messages = listOf(message),
        max_tokens = max_tokens,
        temperature = Temperature.ONE,
        top_p = 1.0,
        n = 1,
        stop = null,
        presence_penalty = 0.0,
        frequency_penalty = 0.0,
        logit_bias = null,
        user = null,
        stream = stream
    )

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
    data class JsonSchema(val strict: Boolean?, val json_schema: Map<String, Any>) : ResponseFormat() {
        internal var schemaName: String = "response"
            private set

        constructor(json_schema: JsonSchemaSpec) : this(json_schema.strict, json_schema.schema) {
            schemaName = json_schema.name
        }

        internal constructor(name: String, strict: Boolean?, json_schema: Map<String, Any>) : this(strict, json_schema) {
            schemaName = name
        }

        val name get() = schemaName
    }
}

@JsonSerializable
data class JsonSchemaSpec(
    val name: String,
    val schema: Map<String, Any>,
    val strict: Boolean? = null
)

@JsonSerializable
data class Message(
    val role: Role?,
    val content: List<MessageContent>? = null,
    val name: User? = null,
    val refusal: String? = null,
    val tool_call_id: String? = null,
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

        fun ToolCalls(tool_calls: List<ToolCall>) = Message(Role.Assistant, null, null, null, null, tool_calls)

        fun ToolCallResult(tool_call_id: String, content: String) =
            Message(Role.Tool, listOf(MessageContent(ContentType.text, content)), tool_call_id = tool_call_id)
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
    val finish_reason: StopReason?,
    val logprobs: ChoiceLogProbs? = null
) {
    val message get() = msg ?: delta ?: ChoiceDetail(Role.Assistant, "", emptyList())
}

@JsonSerializable
data class ChoiceLogProbs(
    val content: List<TokenLogProb>? = null
)

@JsonSerializable
data class TokenLogProb(
    val token: String,
    val logprob: Double,
    val bytes: List<Int>? = null,
    val top_logprobs: List<TopLogProb>? = null
)

@JsonSerializable
data class TopLogProb(
    val token: String,
    val logprob: Double,
    val bytes: List<Int>? = null
)

@JsonSerializable
data class ChoiceDetail(
    @JsonProperty(name = "role")
    internal val r: Role?,
    val content: String? = null,
    val tool_calls: List<ToolCall>? = null,
    val reasoning: String? = null,
    val reasoning_content: String? = null
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
data class Tool(val function: FunctionSpec, val type: String = "function")

@JsonSerializable
data class FunctionSpec(
    val name: String,
    val parameters: Map<String, Any>,
    val description: String? = null,
) {
    val type: String = "function"
}

@JsonSerializable
data class FunctionCall(
    val name: String,
    val arguments: String
)

@JsonSerializable
data class CompletionResponse(
    @JsonProperty(name = "id")
    internal val blankId: String,
    val created: Timestamp,
    @JsonProperty(name = "model")
    internal val blankModel: String,
    val choices: List<Choice>,
    @JsonProperty(name = "object")
    internal val blankObjectType: String,
    val usage: Usage? = null,
    val system_fingerprint: String? = null,
    val service_tier: String? = null
) {
    val id get() = ResponseId.of(blankId.takeIf { it.isNotBlank() } ?: "-")
    val model get() = ModelName.of(blankModel.takeIf { it.isNotBlank() } ?: "-")
    val objectType get() = ObjectType.of(blankObjectType.takeIf { it.isNotBlank() } ?: "-")
}
