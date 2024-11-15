package org.http4k.connect.anthropic.action

import dev.forkhandles.result4k.map
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.anthropic.AnthropicAIAction
import org.http4k.connect.anthropic.AnthropicAIMoshi
import org.http4k.connect.anthropic.Prompt
import org.http4k.connect.anthropic.StopReason
import org.http4k.connect.anthropic.ToolChoice
import org.http4k.connect.anthropic.action.MessageGenerationEvent.Ping
import org.http4k.connect.model.ModelName
import org.http4k.connect.model.Role
import org.http4k.connect.util.toCompletionSequence
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.with
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.Polymorphic
import se.ansman.kotshi.PolymorphicLabel

@Http4kConnectAction
@JsonSerializable
@ConsistentCopyVisibility
data class MessageCompletionStream internal constructor(
    override val model: ModelName,
    override val messages: List<Message>,
    override val max_tokens: Int,
    override val metadata: Metadata? = null,
    override val stop_sequences: List<String> = emptyList(),
    override val system: Prompt? = null,
    override val temperature: Double? = 0.0,
    override val tool_choice: ToolChoice? = null,
    override val tools: List<Tool> = emptyList(),
    override val top_k: Int? = 0,
    override val top_p: Double? = 0.0,
    override val stream: Boolean,
) : AbstractMessageCompletion, AnthropicAIAction<Sequence<MessageGenerationEvent>> {
    constructor(
        model: ModelName,
        prompt: Prompt,
        max_tokens: Int,
        metadata: Metadata? = null,
        stop_sequences: List<String> = emptyList(),
        system: Prompt? = null,
        temperature: Double? = 0.0,
        tool_choice: ToolChoice? = null,
        tools: List<Tool> = emptyList(),
        top_k: Int? = 0,
        top_p: Double? = 0.0,
    ) : this(
        model,
        listOf(Message(Role.User, listOf(Content.Text(prompt.value))),),
        max_tokens,
        metadata,
        stop_sequences,
        system,
        temperature,
        tool_choice,
        tools,
        top_k,
        top_p,
        true
    )

    constructor(
        model: ModelName,
        messages: List<Message>,
        max_tokens: Int,
        metadata: Metadata? = null,
        stop_sequences: List<String> = emptyList(),
        system: Prompt? = null,
        temperature: Double? = 0.0,
        tool_choice: ToolChoice? = null,
        tools: List<Tool> = emptyList(),
        top_k: Int? = 0,
        top_p: Double? = 0.0,
    ) : this(
        model,
        messages,
        max_tokens,
        metadata,
        stop_sequences,
        system,
        temperature,
        tool_choice,
        tools,
        top_k,
        top_p,
        true
    )

    override fun toRequest() =
        Request(POST, "/v1/messages").with(AnthropicAIMoshi.autoBody<MessageCompletionStream>().toLens() of this)

    override fun toResult(response: Response) =
        toCompletionSequence(response, AnthropicAIMoshi, "data: ", "event: message_stop")
            .map { it.filterNot { it is Ping } }
}

@JsonSerializable
@Polymorphic("type")
sealed class MessageGenerationEvent : GeneratedContent {

    @JsonSerializable
    @PolymorphicLabel("message_start")
    data class StartMessage(val message: MessageCompletionResponse) : MessageGenerationEvent()

    @JsonSerializable
    @PolymorphicLabel("content_block_start")
    data class StartBlock(val index: Long, val content_block: DeltaContent) : MessageGenerationEvent()

    @JsonSerializable
    @PolymorphicLabel("content_block_delta")
    data class Delta(val index: Long, val delta: DeltaContent) : MessageGenerationEvent()

    @JsonSerializable
    @PolymorphicLabel("content_block_stop")
    data class Stop(val index: Long) : MessageGenerationEvent()

    @JsonSerializable
    @PolymorphicLabel("error")
    data class Error(val index: Long) : MessageGenerationEvent()

    @JsonSerializable
    @PolymorphicLabel("ping")
    data object Ping : MessageGenerationEvent()

    @JsonSerializable
    @PolymorphicLabel("message_delta")
    data class MessageDelta(val delta: MessageDeltaContent) : MessageGenerationEvent()

}

@JsonSerializable
data class MessageDeltaContent(
    val stop_reason: StopReason?,
    val stop_sequence: String?,
    val usage: Usage?
)

@JsonSerializable
@Polymorphic("type")
sealed class DeltaContent {
    @JsonSerializable
    @PolymorphicLabel("text")
    data class Text(val text: String) : DeltaContent()

    @JsonSerializable
    @PolymorphicLabel("text_delta")
    data class TextDelta(val text: String) : DeltaContent()

    @JsonSerializable
    @PolymorphicLabel("input_json_delta")
    data class Json(val partial_json: String) : DeltaContent()
}
