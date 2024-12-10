package org.http4k.connect.azure

import org.http4k.client.JavaHttpClient
import org.http4k.config.Environment
import org.http4k.config.EnvironmentKey
import org.http4k.filter.debug
import org.http4k.lens.value
import org.http4k.util.PortBasedTest
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Disabled

class RealGitHubModelsTest : AzureAIContract, PortBasedTest {
    val token = EnvironmentKey.value(GitHubToken).optional("GITHUB_TOKEN")

    init {
        Assumptions.assumeTrue(token(Environment.ENV) != null, "No API Key set - skipping")
    }

    override val azureAi = AzureAI.Http(
        token(Environment.ENV)!!,
        JavaHttpClient().debug()
    )

    @Disabled("not in github")
    override fun `get completion response streaming`() {
    }

    @Disabled("not in github")
    override fun `get completion response non-stream`() {
    }
}
