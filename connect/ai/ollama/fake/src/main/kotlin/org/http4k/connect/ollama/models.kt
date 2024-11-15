package org.http4k.connect.ollama

import org.http4k.connect.model.ModelName
import org.http4k.connect.ollama.action.Details
import org.http4k.connect.ollama.action.Model
import org.http4k.connect.storage.InMemory
import org.http4k.connect.storage.Storage

val mistral = ModelName.of("mistral:latest")
val gemma = ModelName.of("gemma:2b")

val DEFAULT_OLLAMA_MODELS = Storage.InMemory<Model>().apply {
    setOf(mistral, gemma).forEach {
        val details = Details("gguf", "100B", "Q4_0", "llama", null, listOf("llama"))
        set(it.value, Model(it, 0, "digest", details, null, null, null))
    }
}
