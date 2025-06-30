package org.http4k.connect.anthropic

import org.http4k.ai.model.ApiKey
import org.http4k.filter.debug

class FakeAnthropicAITest : AnthropicAIContract {
    override val anthropicAi = AnthropicAI.Http(
        ApiKey.of("hello"),
        ApiVersion._2023_06_01,
        FakeAnthropicAI().debug()
    )
}
