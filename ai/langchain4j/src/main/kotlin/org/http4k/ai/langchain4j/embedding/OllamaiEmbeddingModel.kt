package org.http4k.ai.langchain4j.embedding

import dev.forkhandles.result4k.allValues
import dev.forkhandles.result4k.map
import dev.langchain4j.data.embedding.Embedding
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.output.Response
import org.http4k.ai.model.ModelName
import org.http4k.ai.model.UserPrompt
import org.http4k.connect.ollama.Ollama
import org.http4k.connect.ollama.createEmbeddings
import org.http4k.connect.orThrow

fun OllamaiEmbeddingModel(ollama: Ollama, model: ModelName) = object : EmbeddingModel {
    override fun embedAll(segments: List<TextSegment>?) =
        (segments ?: emptyList()).map { ollama.createEmbeddings(model, UserPrompt.of(it.text())) }
            .allValues()
            .map { Response(it.map { Embedding(it.embedding) }) }
            .orThrow()
}
