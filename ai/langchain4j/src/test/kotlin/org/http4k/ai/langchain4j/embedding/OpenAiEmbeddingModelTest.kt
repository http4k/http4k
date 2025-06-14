package org.http4k.ai.langchain4j.embedding

import org.http4k.ai.model.ApiKey
import org.http4k.connect.openai.FakeOpenAI
import org.http4k.connect.openai.Http
import org.http4k.connect.openai.OpenAI

class OpenAiEmbeddingModelTest : EmbeddingModelContract {
    override val model = OpenAIEmbeddingModel(OpenAI.Http(ApiKey.of("hello"), FakeOpenAI()))
}
