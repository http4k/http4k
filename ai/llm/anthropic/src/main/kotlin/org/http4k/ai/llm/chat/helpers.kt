package org.http4k.ai.llm.chat

import org.http4k.ai.llm.model.Message
import org.http4k.ai.llm.model.Resource
import org.http4k.ai.llm.chat.ToolSelection.Auto
import org.http4k.ai.llm.chat.ToolSelection.Required
import org.http4k.ai.llm.tools.LLMTool
import org.http4k.ai.llm.tools.ToolRequest
import org.http4k.ai.model.RequestId
import org.http4k.ai.model.TokenUsage
import org.http4k.connect.anthropic.SourceType.base64
import org.http4k.connect.anthropic.ToolChoice
import org.http4k.connect.anthropic.ToolUseId
import org.http4k.connect.anthropic.action.Content
import org.http4k.connect.anthropic.action.Message.Companion.Assistant
import org.http4k.connect.anthropic.action.Message.Companion.System
import org.http4k.connect.anthropic.action.Message.Companion.User
import org.http4k.connect.anthropic.action.MessageCompletionResponse
import org.http4k.connect.anthropic.action.Source
import org.http4k.connect.anthropic.action.Tool
import org.http4k.connect.model.MimeType.Companion.IMAGE_JPG

fun MessageCompletionResponse.toMetadata() =
    ChatResponse.Metadata(id, model, TokenUsage(usage.input_tokens, usage.output_tokens))

fun List<Content>.toLLM() = Message.Assistant(
    filterNot { it is Content.ToolUse }
        .map(Content::toLLM),
    filterIsInstance<Content.ToolUse>()
        .map { ToolRequest(RequestId.of(it.id.value), it.name, it.input) })

fun Message.toAnthropic() = when (this) {
    is Message.Assistant -> Assistant(
        contents.map { it.toAnthropic() } + toolRequests.map { it.toAnthropic() }
    )

    is Message.System -> System(Content.Text(text))
    is Message.ToolResult -> User(Content.ToolResult(ToolUseId.of(id.value), text))
    is Message.User -> User(contents.map { it.toAnthropic() })
    is Message.Custom -> error("Unsupported message type: ${this::class.simpleName}")
}

fun Resource.toAnthropic(): Source = when (this) {
    is Resource.Binary -> Source(content, mimeType ?: IMAGE_JPG, base64)
    is Resource.Ref -> error("Unsupported resource type: ${this::class.simpleName}")
}

fun ToolRequest.toAnthropic() = Content.ToolUse(name, ToolUseId.of(id.value), arguments)

fun org.http4k.ai.llm.model.Content.toAnthropic() = when (this) {
    is org.http4k.ai.llm.model.Content.Image -> Content.Image(image.toAnthropic())
    is org.http4k.ai.llm.model.Content.Text -> Content.Text(text)
    else -> error("Unsupported content type: ${this::class.simpleName}")
}

fun Content.toLLM() = when (this) {
    is Content.Image -> org.http4k.ai.llm.model.Content.Image(Resource.Binary(source.data, source.media_type))
    is Content.Text -> org.http4k.ai.llm.model.Content.Text(text)
    else -> error("Unsupported content type: ${this::class.simpleName}")
}

fun ToolSelection.toLLM() = when (this) {
    Auto -> ToolChoice.Auto(true)
    Required -> ToolChoice.Auto(true)
}

fun LLMTool.toAnthropic() = Tool(name, description, inputSchema)
