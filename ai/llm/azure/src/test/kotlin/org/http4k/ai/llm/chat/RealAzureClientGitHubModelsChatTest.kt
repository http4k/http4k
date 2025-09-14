package org.http4k.ai.llm.chat

import org.http4k.ai.model.ApiKey
import org.http4k.ai.model.ModelName
import org.http4k.client.JavaHttpClient
import org.http4k.config.Environment.Companion.ENV
import org.http4k.config.EnvironmentKey
import org.http4k.filter.debug
import org.http4k.lens.value
import org.http4k.util.PortBasedTest
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled("need up to date model list")
class RealAzureClientGitHubModelsChatTest : ChatContract, StreamingChatContract, PortBasedTest {

    val apiKey = EnvironmentKey.value(ApiKey).optional("GITHUB_MODELS_TOKEN")

    init {
        assumeTrue(apiKey(ENV) != null, "No API Key set - skipping")
    }

    override val chat = Chat.AzureGitHubModels(apiKey(ENV)!!, JavaHttpClient().debug())
    override val streamingChat = StreamingChat.AzureGitHubModels(apiKey(ENV)!!, JavaHttpClient().debug())

    override val model = ModelName.of("Phi-3-small-8k-instruct")

    @Test
    @Disabled
    override fun `can generate a tool request`() {
    }
}
