package org.http4k.connect.langchain.chat

import org.http4k.connect.langchain.image.OpenAiImageModel
import org.http4k.connect.openai.FakeOpenAI
import org.http4k.connect.openai.Http
import org.http4k.connect.openai.OpenAI
import org.http4k.connect.openai.OpenAIToken

class OpenAiImageModelTest : ImageModelContract {
    override val model = OpenAiImageModel(OpenAI.Http(OpenAIToken.of("hello"), FakeOpenAI()))
}
