package org.http4k.ai.llm.chat

import org.http4k.ai.llm.chat.ChatResponseFormat.Json
import org.http4k.ai.llm.chat.ChatResponseFormat.Text
import org.http4k.ai.llm.model.Content
import org.http4k.ai.llm.model.Message
import org.http4k.ai.llm.model.Message.Assistant
import org.http4k.ai.llm.model.Resource
import org.http4k.ai.llm.tools.LLMTool
import org.http4k.ai.llm.tools.ToolRequest
import org.http4k.ai.llm.util.LLMJson
import org.http4k.ai.model.RequestId
import org.http4k.ai.model.Temperature
import org.http4k.ai.model.TokenUsage
import org.http4k.ai.model.ToolName
import org.http4k.connect.openai.action.ChatCompletion
import org.http4k.connect.openai.action.Choice
import org.http4k.connect.openai.action.CompletionResponse
import org.http4k.connect.openai.action.ContentType
import org.http4k.connect.openai.action.ContentType.image_url
import org.http4k.connect.openai.action.FunctionCall
import org.http4k.connect.openai.action.FunctionSpec
import org.http4k.connect.openai.action.ImageUrl
import org.http4k.connect.openai.action.Message.Companion.Assistant
import org.http4k.connect.openai.action.Message.Companion.System
import org.http4k.connect.openai.action.Message.Companion.ToolCallResult
import org.http4k.connect.openai.action.Message.Companion.ToolCalls
import org.http4k.connect.openai.action.Message.Companion.User
import org.http4k.connect.openai.action.MessageContent
import org.http4k.connect.openai.action.ResponseFormat.JsonSchema
import org.http4k.connect.openai.action.Tool
import org.http4k.connect.openai.action.ToolCall
import java.util.UUID

fun ChatResponseFormat.toOpenAI() = when (this) {
    is Json -> JsonSchema(null, LLMJson.convert(schema))
    Text -> null
}

fun CompletionResponse.toHttp4k() = ChatResponse(
    choices.toHttp4k(),
    ChatResponse.Metadata(
        id, model, usage
            ?.let { TokenUsage(it.prompt_tokens, it.completion_tokens) })
)

fun List<Choice>.toHttp4k(): Assistant {
    val (contents, tools) = partition { (it.message.tool_calls?.size ?: 0) == 0 }
    return Assistant(
        contents.map { Content.Text(it.message.content ?: "") },
        tools.flatMap { it.message.tool_calls?.map { it.toHttp4k() } ?: emptyList() }
    )
}

fun ToolCall.toHttp4k() =
    ToolRequest(
        if (id.isBlank()) RequestId.of(UUID.randomUUID().toString()) else RequestId.of(id),
        ToolName.of(function.name),
        LLMJson.convert(LLMJson.parse(function.arguments))
    )

fun LLMTool.toOpenAI() = Tool(FunctionSpec(name.value, inputSchema, description))


fun Message.toOpenAI(): org.http4k.connect.openai.action.Message = when (this) {
    is Message.Assistant -> {
        when {
            toolRequests.isEmpty() -> Assistant(contents.map { it.toOpenAI() })
            else -> ToolCalls(toolRequests.map(ToolRequest::toOpenAI))
        }
    }

    is Message.System -> System(text)
    is Message.ToolResult -> ToolCallResult(id.value, text)
    is Message.User -> User(contents.map { it.toOpenAI() })
    is Message.Custom -> error("Unsupported message type $this")
}


fun ToolRequest.toOpenAI() = ToolCall(id.value, "function", FunctionCall(name.value, LLMJson.asFormatString(arguments)))

fun Content.toOpenAI() = when (this) {
    is Content.Image -> {
        val resource = image
        MessageContent(
            image_url, image_url = when (resource) {
                is Resource.Binary -> error("Unsupported binary type $image")
                is Resource.Ref -> ImageUrl(resource.uri)
            }
        )
    }

    is Content.Text -> MessageContent(ContentType.text, text)
    else -> error("Unsupported content type $this")
}

fun ChatRequest.toOpenAI(stream: Boolean) =
    ChatCompletion(
        params.modelName,
        messages.map { it.toOpenAI() },
        params.maxOutputTokens,
        params.temperature ?: Temperature.ONE,
        params.topP,
        1,
        params.stopSequences,
        params.presencePenalty,
        params.frequencyPenalty,
        null,
        null,
        stream,
        params.responseFormat?.toOpenAI(),
        params.tools.map { it.toOpenAI() }
    )
