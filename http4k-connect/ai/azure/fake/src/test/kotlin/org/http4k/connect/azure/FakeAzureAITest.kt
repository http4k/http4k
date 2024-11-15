package org.http4k.connect.azure

class FakeAzureAITest : AzureAIContract {
    private val fakeOpenAI = FakeAzureAI()
    override val azureAi = AzureAI.Http(AzureAIApiKey.of("hello"),
        AzureHost.of("foobar"), Region.of("barfoo"),
        fakeOpenAI)
}
