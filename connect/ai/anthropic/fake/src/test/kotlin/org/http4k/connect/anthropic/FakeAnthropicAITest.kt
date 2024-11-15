package org.http4k.connect.anthropic

import org.http4k.filter.debug

class FakeAnthropicAITest : AnthropicAIContract {
    override val anthropicAi = AnthropicAI.Http(
        AnthropicIApiKey.of("hello"),
        ApiVersion._2023_06_01,
        FakeAnthropicAI().debug())
}
