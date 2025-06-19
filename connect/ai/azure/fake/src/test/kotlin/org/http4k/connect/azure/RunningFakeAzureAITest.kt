package org.http4k.connect.azure

import org.http4k.ai.model.ApiKey
import org.http4k.connect.WithRunningFake

class RunningFakeAzureAITest : AzureAIContract, WithRunningFake(::FakeAzureAI) {
    override val azureAi = AzureAI.Http(
        ApiKey.of("hello"),
        AzureHost.of("foobar"), Region.of("barfoo"),
        http)
}
