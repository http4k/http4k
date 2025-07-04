package org.http4k.ai.langchain4j.chat

import org.http4k.ai.langchain4j.image.OpenAIImageModel
import org.http4k.ai.model.ApiKey
import org.http4k.connect.openai.FakeOpenAI
import org.http4k.connect.openai.Http
import org.http4k.connect.openai.OpenAI

class OpenAiImageModelTest : ImageModelContract {
    override val model = OpenAIImageModel(OpenAI.Http(ApiKey.of("hello"), FakeOpenAI()))
}
