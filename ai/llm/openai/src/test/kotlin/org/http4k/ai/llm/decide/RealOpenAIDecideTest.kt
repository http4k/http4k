package org.http4k.ai.llm.decide

import org.http4k.ai.model.ApiKey
import org.http4k.client.JavaHttpClient
import org.http4k.config.Environment.Companion.ENV
import org.http4k.config.EnvironmentKey
import org.http4k.connect.openai.OpenAIModels
import org.http4k.filter.debug
import org.http4k.lens.value
import org.http4k.util.PortBasedTest
import org.junit.jupiter.api.Assumptions.assumeTrue

class RealOpenAIDecideTest : DecideContract, PortBasedTest {

    val apiKey = EnvironmentKey.value(ApiKey).optional("OPENAI_API_KEY")

    init {
        assumeTrue(apiKey(ENV) != null, "No API Key set - skipping")
    }

    override val decide = Decide.OpenAI(apiKey(ENV)!!, JavaHttpClient().debug())
    override val model = OpenAIModels.GPT_6_LUNA
}
