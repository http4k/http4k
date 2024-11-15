package org.http4k.connect.anthropic

import org.http4k.connect.WithRunningFake

class RunningFakeAnthropicAITest : AnthropicAIContract, WithRunningFake(::FakeAnthropicAI) {
    override val anthropicAi = AnthropicAI.Http(
        AnthropicIApiKey.of("hello"),
        ApiVersion._2023_06_01,
        http
    )
}
