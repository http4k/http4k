package org.http4k.connect.azure

import org.http4k.ai.model.ApiKey
import org.http4k.client.JavaHttpClient
import org.http4k.config.Environment
import org.http4k.config.Environment.Companion.ENV
import org.http4k.config.EnvironmentKey
import org.http4k.filter.debug
import org.http4k.lens.value
import org.http4k.util.PortBasedTest
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Disabled

class RealGitHubModelsTest : AzureAIContract, PortBasedTest {
    val token = EnvironmentKey.value(ApiKey).optional("GITHUB_TOKEN_MODELS")

    init {
        Assumptions.assumeTrue(token(ENV) != null, "No API Key set - skipping")
    }

    override val azureAi = AzureAI.Http(
        token(ENV)!!,
        JavaHttpClient().debug()
    )

    @Disabled("not in github")
    override fun `get completion response streaming`() {
    }

    @Disabled("not in github")
    override fun `get completion response non-stream`() {
    }
}
