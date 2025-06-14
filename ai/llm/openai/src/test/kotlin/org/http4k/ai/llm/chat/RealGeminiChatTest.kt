package org.http4k.ai.llm.chat

import org.http4k.ai.llm.Gemini
import org.http4k.client.JavaHttpClient
import org.http4k.config.Environment.Companion.ENV
import org.http4k.config.EnvironmentKey
import org.http4k.filter.debug
import org.http4k.lens.value
import org.http4k.util.PortBasedTest
import org.junit.jupiter.api.Assumptions.assumeTrue

class RealGeminiChatTest : ChatContract, StreamingChatContract, PortBasedTest {

    val apiKey = EnvironmentKey.value(Gemini.ApiKey).optional("GEMINI_API_KEY")

    init {
        assumeTrue(apiKey(ENV) != null, "No API Key set - skipping")
    }

    override val chat = Chat.OpenAI(Gemini(apiKey(ENV)!!, JavaHttpClient().debug()))
    override val streamingChat = StreamingChat.OpenAI(Gemini(apiKey(ENV)!!, JavaHttpClient().debug()))

    override val model = Gemini.Models.Gemini1_5
}
