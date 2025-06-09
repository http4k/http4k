package org.http4k.ai.llm.chat

import org.http4k.ai.llm.OpenAIApi
import org.http4k.client.JavaHttpClient
import org.http4k.config.Environment.Companion.ENV
import org.http4k.config.EnvironmentKey
import org.http4k.connect.openai.OpenAIModels.GPT3_5
import org.http4k.filter.debug
import org.http4k.lens.value
import org.http4k.util.PortBasedTest
import org.junit.jupiter.api.Assumptions.assumeTrue

class RealOpenAIChatTest : ChatContract, StreamingChatContract, PortBasedTest {

    val apiKey = EnvironmentKey.value(OpenAIApi.ApiKey).optional("OPENAI_API_KEY")

    init {
        assumeTrue(apiKey(ENV) != null, "No API Key set - skipping")
    }

    override val chat = Chat.OpenAI(apiKey(ENV)!!, JavaHttpClient().debug())
    override val streamingChat = StreamingChat.OpenAI(apiKey(ENV)!!, JavaHttpClient().debug())

    override val model = GPT3_5
}

