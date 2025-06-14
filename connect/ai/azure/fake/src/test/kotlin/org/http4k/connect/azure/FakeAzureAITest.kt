package org.http4k.connect.azure

import org.http4k.ai.model.ApiKey

class FakeAzureAITest : AzureAIContract {
    private val fakeOpenAI = FakeAzureAI()
    override val azureAi = AzureAI.Http(
        ApiKey.of("hello"),
        AzureHost.of("foobar"), Region.of("barfoo"),
        fakeOpenAI
    )
}
