package org.http4k.connect.langchain.embedding

import org.http4k.connect.openai.FakeOpenAI
import org.http4k.connect.openai.Http
import org.http4k.connect.openai.OpenAI
import org.http4k.connect.openai.OpenAIToken

class OpenAiEmbeddingModelTest : EmbeddingModelContract {
    override val model = OpenAiEmbeddingModel(OpenAI.Http(OpenAIToken.of("hello"), FakeOpenAI()))
}
