package org.http4k.ai.langchain4j.embedding

import dev.forkhandles.result4k.map
import dev.langchain4j.data.embedding.Embedding
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.output.Response
import org.http4k.ai.model.ModelName
import org.http4k.connect.openai.OpenAI
import org.http4k.connect.openai.OpenAIModels
import org.http4k.connect.openai.createEmbeddings
import org.http4k.connect.orThrow

fun OpenAIEmbeddingModel(openAi: OpenAI, model: ModelName = OpenAIModels.TEXT_EMBEDDING_ADA_002) = object : EmbeddingModel {
    override fun embedAll(segments: List<TextSegment>?) =
        openAi.createEmbeddings(model, segments?.map { it.text() } ?: emptyList())
            .map { Response(it.data.map { Embedding(it.embedding) }) }
            .orThrow()
}
