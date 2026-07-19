package org.http4k.ai.langchain4j.embedding

import dev.forkhandles.result4k.map
import dev.langchain4j.data.embedding.Embedding
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.output.Response
import org.http4k.ai.model.ModelName
import org.http4k.connect.lmstudio.LmStudio
import org.http4k.connect.lmstudio.createEmbeddings
import org.http4k.connect.orThrow

fun LmStudioEmbeddingModel(lmStudio: LmStudio, model: ModelName) = object : EmbeddingModel {
    override fun embedAll(segments: List<TextSegment>?) =
        lmStudio.createEmbeddings(model, segments?.map { it.text() } ?: emptyList())
            .map { Response(it.data.map { Embedding(it.embedding) }) }
            .orThrow()
}
