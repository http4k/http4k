package org.http4k.ai.llm.chat

import org.http4k.ai.model.ApiKey
import org.http4k.ai.model.ReasoningEffort
import org.http4k.client.JavaHttpClient
import org.http4k.config.Environment.Companion.ENV
import org.http4k.config.EnvironmentKey
import org.http4k.connect.openai.OpenAIModels.GPT_6_LUNA
import org.http4k.filter.debug
import org.http4k.lens.value
import org.http4k.util.PortBasedTest
import org.junit.jupiter.api.Assumptions.assumeTrue

class RealOpenAIChatTest : ChatContract, StreamingChatContract, PortBasedTest {

    val apiKey = EnvironmentKey.value(ApiKey).optional("OPENAI_API_KEY")

    init {
        assumeTrue(apiKey(ENV) != null, "No API Key set - skipping")
    }

    private val chatClient = Chat.OpenAI(apiKey(ENV)!!, JavaHttpClient().debug())
    private val streamingChatClient = StreamingChat.OpenAI(apiKey(ENV)!!, JavaHttpClient().debug())

    override val chat = Chat { chatClient(it.withoutReasoning()) }
    override val streamingChat = StreamingChat { streamingChatClient(it.withoutReasoning()) }

    override val model = GPT_6_LUNA
}

private fun ChatRequest.withoutReasoning() = copy(params = params.copy(reasoningEffort = ReasoningEffort.NONE))
