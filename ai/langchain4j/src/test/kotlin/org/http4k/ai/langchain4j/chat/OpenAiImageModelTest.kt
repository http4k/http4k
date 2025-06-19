package org.http4k.ai.langchain4j.chat

import org.http4k.ai.langchain4j.image.OpenAIImageModel
import org.http4k.connect.openai.FakeOpenAI
import org.http4k.connect.openai.Http
import org.http4k.connect.openai.OpenAI
import org.http4k.connect.openai.OpenAIToken

class OpenAiImageModelTest : ImageModelContract {
    override val model = OpenAIImageModel(OpenAI.Http(OpenAIToken.of("hello"), FakeOpenAI()))
}
