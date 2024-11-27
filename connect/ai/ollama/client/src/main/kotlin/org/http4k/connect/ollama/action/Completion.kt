package org.http4k.connect.ollama.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.model.Base64Blob
import org.http4k.connect.model.ModelName
import org.http4k.connect.ollama.OllamaAction
import org.http4k.connect.ollama.OllamaMoshi
import org.http4k.connect.ollama.OllamaMoshi.autoBody
import org.http4k.connect.ollama.Prompt
import org.http4k.connect.ollama.ResponseFormat
import org.http4k.connect.ollama.SystemMessage
import org.http4k.connect.ollama.Template
import org.http4k.connect.util.toCompletionSequence
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.with
import se.ansman.kotshi.JsonSerializable
import java.time.Instant

@Http4kConnectAction
@JsonSerializable
data class Completion(
    val model: ModelName,
    val prompt: Prompt,
    val images: List<Base64Blob>? = null,
    val stream: Boolean? = false,
    val system: SystemMessage? = null,
    val format: ResponseFormat? = null,
    val template: Template? = null,
    val raw: Boolean? = null,
    val keep_alive: String? = null,
    val options: ModelOptions? = null
) : OllamaAction<Sequence<CompletionResponse>> {

    override fun toRequest() = Request(POST, "/api/generate")
        .with(autoBody<Completion>().toLens() of this)

    override fun toResult(response: Response) = toCompletionSequence(response, OllamaMoshi, "", "__FAKE_HTTP4k_STOP_SIGNAL__")
}

@JsonSerializable
data class CompletionResponse(
    val model: ModelName,
    val created_at: Instant,
    val response: String?,
    val done: Boolean,
    val context: List<Long>? = null,
    val total_duration: Long? = null,
    val load_duration: Long? = null,
    val prompt_eval_count: Long? = null,
    val prompt_eval_duration: Long? = null,
    val eval_count: Long? = null,
    val eval_duration: Long? = null
)
