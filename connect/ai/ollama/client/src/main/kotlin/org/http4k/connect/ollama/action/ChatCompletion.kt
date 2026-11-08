package org.http4k.connect.ollama.action

import org.http4k.ai.model.ModelName
import org.http4k.ai.util.toJsonLinesSequence
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.ollama.Message
import org.http4k.connect.ollama.OllamaAction
import org.http4k.connect.ollama.OllamaMoshi
import org.http4k.connect.ollama.OllamaMoshi.autoBody
import org.http4k.connect.ollama.ResponseFormat
import org.http4k.connect.ollama.Tool
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.with
import se.ansman.kotshi.JsonSerializable
import java.time.Instant

@Http4kConnectAction
@JsonSerializable
data class ChatCompletion(
    val model: ModelName,
    val messages: List<Message>,
    val stream: Boolean? = false,
    val format: ResponseFormat? = null,
    val keep_alive: String? = null,
    val options: ModelOptions? = null,
    val tools: List<Tool>? = null
) : OllamaAction<Sequence<ChatCompletionResponse>> {
    // Retain the original constructor, including its default-argument JVM entry point.
    constructor(
        model: ModelName,
        messages: List<Message>,
        stream: Boolean? = false,
        format: ResponseFormat? = null,
        keep_alive: String? = null,
        options: ModelOptions? = null
    ) : this(model, messages, stream, format, keep_alive, options, null)

    constructor(
        model: ModelName,
        messages: Message,
        stream: Boolean? = false,
        format: ResponseFormat? = null,
        keep_alive: String? = null,
        options: ModelOptions? = null
    ) : this(model, listOf(messages), stream, format, keep_alive, options)

    constructor(
        model: ModelName,
        messages: Message,
        stream: Boolean? = false,
        format: ResponseFormat? = null,
        keep_alive: String? = null,
        options: ModelOptions? = null,
        tools: List<Tool>? = null
    ) : this(model, listOf(messages), stream, format, keep_alive, options, tools)

    // Keep existing compiled copy() calls working, without dropping tool definitions.
    fun copy(
        model: ModelName = this.model,
        messages: List<Message> = this.messages,
        stream: Boolean? = this.stream,
        format: ResponseFormat? = this.format,
        keep_alive: String? = this.keep_alive,
        options: ModelOptions? = this.options
    ) = ChatCompletion(model, messages, stream, format, keep_alive, options, tools)

    override fun toRequest() = Request(POST, "/api/chat")
        .with(autoBody<ChatCompletion>().toLens() of this)

    override fun toResult(response: Response) =
        toJsonLinesSequence(response, OllamaMoshi)
}

@JsonSerializable
data class ChatCompletionResponse(
    val model: ModelName,
    val created_at: Instant,
    val message: Message?,
    val done: Boolean,
    val total_duration: Long? = null,
    val load_duration: Long? = null,
    val prompt_eval_count: Long? = null,
    val prompt_eval_duration: Long? = null,
    val eval_count: Long? = null,
    val eval_duration: Long? = null
)
