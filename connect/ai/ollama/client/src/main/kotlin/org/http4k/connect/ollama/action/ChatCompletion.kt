package org.http4k.connect.ollama.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.model.ModelName
import org.http4k.connect.ollama.Message
import org.http4k.connect.ollama.OllamaAction
import org.http4k.connect.ollama.OllamaMoshi
import org.http4k.connect.ollama.OllamaMoshi.autoBody
import org.http4k.connect.ollama.ResponseFormat
import org.http4k.connect.util.toCompletionSequence
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
    val options: ModelOptions? = null
) : OllamaAction<Sequence<ChatCompletionResponse>> {
    constructor(
        model: ModelName,
        messages: Message,
        stream: Boolean? = false,
        format: ResponseFormat? = null,
        keep_alive: String? = null,
        options: ModelOptions? = null
    ) : this(model, listOf(messages), stream, format, keep_alive, options)

    override fun toRequest() = Request(POST, "/api/chat")
        .with(autoBody<ChatCompletion>().toLens() of this)

    override fun toResult(response: Response) =
        toCompletionSequence(response, OllamaMoshi, "", "__FAKE_HTTP4k_STOP_SIGNAL__")
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
