package org.http4k.connect.langchain.chat

import org.http4k.client.JavaHttpClient
import org.http4k.config.Environment.Companion.ENV
import org.http4k.config.EnvironmentKey
import org.http4k.connect.openai.Http
import org.http4k.connect.openai.OpenAI
import org.http4k.connect.openai.OpenAIToken
import org.http4k.filter.debug
import org.http4k.lens.value
import org.http4k.util.PortBasedTest
import org.junit.jupiter.api.Assumptions.assumeTrue

class RealOpenAiChatLanguageModelTest : ChatLanguageModelContract, PortBasedTest {

    val apiKey = EnvironmentKey.value(OpenAIToken).optional("OPEN_AI_TOKEN")

    init {
        assumeTrue(apiKey(ENV) != null, "No API Key set - skipping")
    }

    override val model by lazy {
        OpenAiChatLanguageModel(
            OpenAI.Http(apiKey(ENV)!!, JavaHttpClient().debug()),
            OpenAiChatModelOptions(temperature = 0.0)
        )
    }
}
