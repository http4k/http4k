package org.http4k.connect.langchain.chat

import org.http4k.connect.openai.FakeOpenAI
import org.http4k.connect.openai.Http
import org.http4k.connect.openai.OpenAI
import org.http4k.connect.openai.OpenAIToken

class OpenAiChatLanguageModelTest : ChatLanguageModelContract {
    override val model by lazy {
        OpenAiChatLanguageModel(
            OpenAI.Http(OpenAIToken.of("hello"), FakeOpenAI()),
            OpenAiChatModelOptions(temperature = 0.0)
        )
    }
}
