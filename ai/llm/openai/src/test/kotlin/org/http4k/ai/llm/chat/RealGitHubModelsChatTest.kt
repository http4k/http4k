package org.http4k.ai.llm.chat

import org.http4k.ai.llm.GitHubModels
import org.http4k.client.JavaHttpClient
import org.http4k.config.Environment.Companion.ENV
import org.http4k.config.EnvironmentKey
import org.http4k.filter.debug
import org.http4k.lens.value
import org.http4k.util.PortBasedTest
import org.junit.jupiter.api.Assumptions.assumeTrue

class RealGitHubModelsChatTest : ChatContract, PortBasedTest {

    val apiKey = EnvironmentKey.value(GitHubModels.ApiKey).optional("GITHUB_MODELS_TOKEN")

    init {
        assumeTrue(apiKey(ENV) != null, "No API Key set - skipping")
    }

    override val chat = Chat.OpenAI(GitHubModels(apiKey(ENV)!!, JavaHttpClient().debug()))

    override val model = GitHubModels.Models.OpenAI_GPT4
}
