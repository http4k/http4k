package org.http4k.connect.openai

import org.http4k.connect.WithRunningFake

class RunningFakeOpenAITest : OpenAIContract, WithRunningFake(::FakeOpenAI) {
    override val openAi = OpenAI.Http(OpenAIToken.of("hello"), http)
}
