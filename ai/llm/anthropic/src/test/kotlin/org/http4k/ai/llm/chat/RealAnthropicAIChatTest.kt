package org.http4k.ai.llm.chat

import org.http4k.ai.model.ApiKey
import org.http4k.client.JavaHttpClient
import org.http4k.config.Environment.Companion.ENV
import org.http4k.config.EnvironmentKey
import org.http4k.connect.anthropic.AnthropicModels.Claude_Sonnet_3_7
import org.http4k.connect.anthropic.ApiVersion.Companion._2023_06_01
import org.http4k.filter.debug
import org.http4k.lens.value
import org.http4k.util.PortBasedTest
import org.junit.jupiter.api.Assumptions.assumeTrue

class RealAnthropicAIChatTest : ChatContract, PortBasedTest {

    val token = EnvironmentKey.value(ApiKey).optional("ANTHROPIC_API_KEY")

    init {
        assumeTrue(token(ENV) != null, "No API Key set - skipping")
    }

    override val chat = Chat.AnthropicAI(token(ENV)!!, JavaHttpClient().debug(), _2023_06_01)
    override val model = Claude_Sonnet_3_7
}
