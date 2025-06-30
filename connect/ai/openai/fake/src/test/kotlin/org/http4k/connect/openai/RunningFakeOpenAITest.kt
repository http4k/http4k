package org.http4k.connect.openai

import org.http4k.ai.model.ApiKey
import org.http4k.connect.WithRunningFake

class RunningFakeOpenAITest : OpenAIContract, WithRunningFake(::FakeOpenAI) {
    override val openAi = OpenAI.Http(ApiKey.of("hello"), http)
}
