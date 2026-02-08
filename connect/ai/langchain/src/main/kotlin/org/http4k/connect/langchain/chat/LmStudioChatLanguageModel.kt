package org.http4k.connect.langchain.chat

import dev.forkhandles.result4k.map
import dev.langchain4j.agent.tool.ToolExecutionRequest
import dev.langchain4j.agent.tool.ToolSpecification
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.ImageContent
import dev.langchain4j.data.message.ImageContent.DetailLevel.AUTO
import dev.langchain4j.data.message.ImageContent.DetailLevel.HIGH
import dev.langchain4j.data.message.ImageContent.DetailLevel.LOW
import dev.langchain4j.data.message.ImageContent.DetailLevel.MEDIUM
import dev.langchain4j.data.message.ImageContent.DetailLevel.ULTRA_HIGH
import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.data.message.TextContent
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.chat.request.ChatRequest
import dev.langchain4j.model.chat.response.ChatResponse
import dev.langchain4j.model.output.FinishReason
import dev.langchain4j.model.output.TokenUsage
import org.http4k.ai.model.MaxTokens
import org.http4k.ai.model.ModelName
import org.http4k.ai.model.Role
import org.http4k.ai.model.StopReason
import org.http4k.ai.model.Temperature
import org.http4k.connect.lmstudio.LmStudio
import org.http4k.connect.lmstudio.TokenId
import org.http4k.connect.lmstudio.User
import org.http4k.connect.lmstudio.action.ContentType
import org.http4k.connect.lmstudio.action.Detail.auto
import org.http4k.connect.lmstudio.action.Detail.high
import org.http4k.connect.lmstudio.action.Detail.low
import org.http4k.connect.lmstudio.action.FunctionCall
import org.http4k.connect.lmstudio.action.FunctionSpec
import org.http4k.connect.lmstudio.action.ImageUrl
import org.http4k.connect.lmstudio.action.Message
import org.http4k.connect.lmstudio.action.MessageContent
import org.http4k.connect.lmstudio.action.ResponseFormat
import org.http4k.connect.lmstudio.action.Tool
import org.http4k.connect.lmstudio.action.ToolCall
import org.http4k.connect.lmstudio.chatCompletion
import org.http4k.connect.openai.*
import org.http4k.connect.orThrow
import org.http4k.core.Uri

data class LmStudioChatModelOptions(
    val model: ModelName,
    val stream: Boolean? = null,
    val maxTokens: MaxTokens? = null,
    val temperature: Temperature = Temperature.ONE,
    val top_p: Double = 1.0,
    val n: Int = 1,
    val stop: List<String>? = null,
    val presencePenalty: Double = 0.0,
    val frequencyPenalty: Double = 0.0,
    val logitBias: Map<TokenId, Double>? = null,
    val user: User? = null,
    val responseFormat: ResponseFormat? = null,
    val toolChoice: Any? = null,
    val parallelToolCalls: Boolean? = null,
)

fun LmStudioChatLanguageModel(lmStudio: LmStudio, options: LmStudioChatModelOptions) =
    object : ChatModel {
        override fun doChat(request: ChatRequest) = with(request) {
            with(options) {
                lmStudio.chatCompletion(
                    ModelName.of(modelName()),
                    messages().map {
                        when (it) {
                            is UserMessage -> it.toHttp4k()
                            is SystemMessage -> it.toHttp4k()
                            is AiMessage -> it.toHttp4k()
                            else -> error("unknown message type")
                        }
                    },
                    MaxTokens.of(1),
                    temperature,
                    top_p,
                    n,
                    stop,
                    presencePenalty,
                    frequencyPenalty,
                    logitBias,
                    user,
                    stream ?: false,
                    responseFormat,
                    toolSpecifications()?.takeIf { it.isNotEmpty() }?.map { it.toHttp4k() },
                    options.toolChoice,
                    options.parallelToolCalls ?: false
                ).map {
                    it.map {
                        ChatResponse.builder()
                            .aiMessage(
                                AiMessage(
                                    it.choices.mapNotNull { it.message?.content }.joinToString("")
                                )
                            )
                            .tokenUsage(it.usage?.let {
                                TokenUsage(
                                    it.prompt_tokens,
                                    it.completion_tokens,
                                    it.total_tokens
                                )
                            })
                            .finishReason(
                                when (it.choices.last().finish_reason) {
                                    StopReason.stop -> FinishReason.STOP
                                    StopReason.length -> FinishReason.LENGTH
                                    StopReason.content_filter -> FinishReason.CONTENT_FILTER
                                    StopReason.tool_calls -> FinishReason.TOOL_EXECUTION
                                    else -> FinishReason.OTHER
                                }
                            )
                            .build()
                    }.toList()
                }.orThrow().first()
            }
        }

        private fun UserMessage.toHttp4k() = Message(
            Role.User,
            contents().map {
                when (it) {
                    is TextContent -> it.toHttp4k()
                    is ImageContent -> it.toHttp4k()
                    else -> error("unknown content type")
                }
            }, name()?.let { User.of(it) },
            null
        )

        private fun SystemMessage.toHttp4k() = Message(
            Role.System,
            listOf(MessageContent(ContentType.text, text()))
        )

        private fun AiMessage.toHttp4k(): Message {
            val toolCalls = toolExecutionRequests()?.map { it.toHttp4k() }?.takeIf { it.isNotEmpty() }
            return Message(Role.Assistant, listOf(MessageContent(ContentType.text, text())), tool_calls = toolCalls)
        }

        private fun ToolExecutionRequest.toHttp4k() = ToolCall(id(), "function", FunctionCall(name(), arguments()))

        private fun TextContent.toHttp4k() = MessageContent(ContentType.text, this@toHttp4k.text())

        private fun ImageContent.toHttp4k() =
            MessageContent(
                ContentType.image_url, null, ImageUrl(
                    Uri.of(this@toHttp4k.image().url().toString()),
                    when (this@toHttp4k.detailLevel()) {
                        LOW, MEDIUM -> low
                        HIGH, ULTRA_HIGH -> high
                        AUTO -> auto
                    }
                )
            )

        private fun ToolSpecification.toHttp4k() = Tool(
            FunctionSpec(
                this@toHttp4k.name(),
                this@toHttp4k.parameters(),
                this@toHttp4k.description()
            )
        )
    }
