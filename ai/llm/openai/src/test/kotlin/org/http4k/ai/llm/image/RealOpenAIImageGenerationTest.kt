package org.http4k.ai.llm.image

import org.http4k.ai.llm.OpenAIApi
import org.http4k.client.JavaHttpClient
import org.http4k.config.Environment
import org.http4k.config.EnvironmentKey
import org.http4k.connect.openai.OpenAIModels.DALL_E_2
import org.http4k.filter.debug
import org.http4k.lens.value
import org.http4k.util.PortBasedTest
import org.junit.jupiter.api.Assumptions.assumeTrue

class RealOpenAIImageGenerationTest : ImageGenerationContract, PortBasedTest {

    val apiKey = EnvironmentKey.value(OpenAIApi.ApiKey).optional("OPENAI_API_KEY2")

    init {
        assumeTrue(apiKey(Environment.ENV) != null, "No API Key set - skipping")
    }

    override val imageGeneration = ImageGeneration.OpenAI(apiKey(Environment.ENV)!!, JavaHttpClient().debug())

    override val model = DALL_E_2
}
