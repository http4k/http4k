package org.http4k.connect.langchain.embedding

import dev.forkhandles.result4k.allValues
import dev.forkhandles.result4k.map
import dev.langchain4j.data.embedding.Embedding
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.output.Response
import org.http4k.connect.model.ModelName
import org.http4k.connect.ollama.Ollama
import org.http4k.connect.ollama.Prompt
import org.http4k.connect.ollama.createEmbeddings
import org.http4k.connect.orThrow

fun OllamaiEmbeddingModel(ollama: Ollama, model: ModelName) = EmbeddingModel {
    it.map { ollama.createEmbeddings(model, Prompt.of(it.text())) }
        .allValues()
        .map { Response(it.map { Embedding(it.embedding) }) }
        .orThrow()
}
