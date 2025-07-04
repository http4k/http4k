package org.http4k.connect.anthropic.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.ai.model.MaxTokens
import org.http4k.ai.model.ModelName
import org.http4k.ai.model.UserPrompt
import org.http4k.ai.model.ResponseId
import org.http4k.ai.model.Role
import org.http4k.ai.model.StopReason
import org.http4k.ai.model.SystemPrompt
import org.http4k.ai.model.Temperature
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.anthropic.AnthropicAIAction
import org.http4k.connect.anthropic.AnthropicAIMoshi
import org.http4k.connect.anthropic.ToolChoice
import org.http4k.connect.asRemoteFailure
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.with
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
@ConsistentCopyVisibility
data class MessageCompletion internal constructor(
    override val model: ModelName,
    override val messages: List<Message>,
    override val max_tokens: MaxTokens,
    override val metadata: Metadata? = null,
    override val stop_sequences: List<String> = emptyList(),
    override val system: SystemPrompt? = null,
    override val temperature: Temperature? = Temperature.ZERO,
    override val tool_choice: ToolChoice? = null,
    override val tools: List<Tool> = emptyList(),
    override val top_k: Int? = 0,
    override val top_p: Double? = 0.0,
    override val stream: Boolean
) : AbstractMessageCompletion, AnthropicAIAction<MessageCompletionResponse> {
    constructor(
        model: ModelName,
        prompt: UserPrompt,
        max_tokens: MaxTokens,
        metadata: Metadata? = null,
        stop_sequences: List<String> = emptyList(),
        system: SystemPrompt? = null,
        temperature: Temperature? = Temperature.ZERO,
        tool_choice: ToolChoice? = null,
        tools: List<Tool> = emptyList(),
        top_k: Int? = 0,
        top_p: Double? = 0.0,
    ) : this(
        model,
        listOf(Message(Role.User, listOf(Content.Text(prompt.value)))),
        max_tokens, metadata, stop_sequences, system, temperature, tool_choice, tools, top_k, top_p, false
    )

    constructor(
        model: ModelName,
        messages: List<Message>,
        max_tokens: MaxTokens,
        metadata: Metadata? = null,
        stop_sequences: List<String> = emptyList(),
        system: SystemPrompt? = null,
        temperature: Temperature? = Temperature.ZERO,
        tool_choice: ToolChoice? = null,
        tools: List<Tool> = emptyList(),
        top_k: Int? = 0,
        top_p: Double? = 0.0,
    ) : this(
        model,
        messages,
        max_tokens, metadata, stop_sequences, system, temperature, tool_choice, tools, top_k, top_p, false
    )

    override fun toRequest() =
        Request(POST, "/v1/messages").with(AnthropicAIMoshi.autoBody<MessageCompletion>().toLens() of this)

    override fun toResult(response: Response) = when {
        response.status.successful -> Success(
            AnthropicAIMoshi.autoBody<MessageCompletionResponse>().toLens()(response)
        )

        else -> Failure(asRemoteFailure(response))
    }

}

@JsonSerializable
data class MessageCompletionResponse(
    val id: ResponseId,
    val role: Role,
    val content: List<Content>,
    val model: ModelName,
    val stop_reason: StopReason?,
    val stop_sequence: String?,
    val usage: Usage
) : GeneratedContent
