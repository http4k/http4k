package org.http4k.connect.langchain.embedding

import dev.forkhandles.result4k.map
import dev.langchain4j.data.embedding.Embedding
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.output.Response
import org.http4k.connect.model.ModelName
import org.http4k.connect.openai.OpenAI
import org.http4k.connect.openai.TEXT_EMBEDDING_ADA_002
import org.http4k.connect.openai.createEmbeddings
import org.http4k.connect.orThrow

fun OpenAiEmbeddingModel(openAi: OpenAI, model: ModelName = ModelName.TEXT_EMBEDDING_ADA_002) = EmbeddingModel {
    openAi.createEmbeddings(model, it?.map { it.text() } ?: emptyList())
        .map { Response(it.data.map { Embedding(it.embedding) }) }
        .orThrow()
}
