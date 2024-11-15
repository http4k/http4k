package org.http4k.connect.ollama.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.model.ModelName
import org.http4k.connect.ollama.OllamaMoshi.autoBody
import org.http4k.connect.ollama.Prompt
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.with
import se.ansman.kotshi.JsonSerializable


@Http4kConnectAction
@JsonSerializable
data class CreateEmbeddings(
    val model: ModelName,
    val prompt: Prompt,
    val keep_alive: String? = null,
    val options: ModelOptions? = null
) : NonNullOllamaAction<EmbeddingsResponse>(EmbeddingsResponse::class) {
    override fun toRequest() = Request(POST, "/api/embeddings")
        .with(autoBody<CreateEmbeddings>().toLens() of this)

}

@JsonSerializable
data class EmbeddingsResponse(val embedding: FloatArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EmbeddingsResponse

        return embedding.contentEquals(other.embedding)
    }

    override fun hashCode(): Int = embedding.contentHashCode()
}
