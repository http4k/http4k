package org.http4k.connect.ollama.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.model.ModelName
import org.http4k.core.Method.GET
import org.http4k.core.Request
import se.ansman.kotshi.JsonSerializable
import java.time.Instant

@Http4kConnectAction
@JsonSerializable
data object GetModels : NonNullOllamaAction<ModelList>(ModelList::class) {
    override fun toRequest() = Request(GET, "/api/ps")
}

@JsonSerializable
data class Details(
    val format: String,
    val parameter_size: String,
    val quantization_level: String,
    val family: String,
    internal val parent_model: String? = null,
    val families: List<String>? = null
) {
    val parentModel = parent_model?.takeIf { it.isNotEmpty() }?.let(ModelName::of)
}

@JsonSerializable
data class Model(
    val name: ModelName,
    val size: Long,
    val digest: String,
    val details: Details,
    val expires_at: Instant? = null,
    val modified_at: Instant? = null,
    val size_vram: Long? = null
)

@JsonSerializable
data class ModelList(val models: List<Model>)
