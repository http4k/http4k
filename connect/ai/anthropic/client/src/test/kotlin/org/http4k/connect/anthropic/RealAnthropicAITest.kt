package org.http4k.connect.anthropic

import org.http4k.client.JavaHttpClient
import org.http4k.config.Environment.Companion.ENV
import org.http4k.config.EnvironmentKey
import org.http4k.filter.debug
import org.http4k.lens.value
import org.http4k.util.PortBasedTest
import org.junit.jupiter.api.Assumptions

class RealAnthropicAITest : AnthropicAIContract, PortBasedTest {
    val token = EnvironmentKey.value(AnthropicIApiKey).optional("ANTHROPIC_API_KEY")

    init {
        Assumptions.assumeTrue(token(ENV) != null, "No API Key set - skipping")
    }

    override val anthropicAi = AnthropicAI.Http(
        token(ENV)!!,
        ApiVersion._2023_06_01,
        JavaHttpClient().debug()
    )
}

