package org.http4k.connect.langchain.chat

import org.http4k.ai.model.ApiKey
import org.http4k.ai.model.Temperature
import org.http4k.connect.openai.FakeOpenAI
import org.http4k.connect.openai.Http
import org.http4k.connect.openai.OpenAI

class OpenAiChatLanguageModelTest : ChatLanguageModelContract {
    override val model by lazy {
        OpenAiChatLanguageModel(
            OpenAI.Http(ApiKey.of("hello"), FakeOpenAI()),
            OpenAiChatModelOptions(temperature = Temperature.ZERO)
        )
    }
}
