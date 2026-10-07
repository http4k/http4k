package org.http4k.ai.llm.image

import org.http4k.ai.model.ApiKey
import org.http4k.client.JavaHttpClient
import org.http4k.config.Environment
import org.http4k.config.EnvironmentKey
import org.http4k.connect.openai.OpenAIModels.GPT_IMAGE_2_5_FLARE
import org.http4k.filter.debug
import org.http4k.lens.value
import org.http4k.util.PortBasedTest
import org.junit.jupiter.api.Assumptions.assumeTrue

class RealOpenAIImageGenerationTest : ImageGenerationContract, PortBasedTest {

    val apiKey = EnvironmentKey.value(ApiKey).optional("OPENAI_API_KEY")

    init {
        assumeTrue(apiKey(Environment.ENV) != null, "No API Key set - skipping")
    }

    override val imageGeneration = ImageGeneration.OpenAI(apiKey(Environment.ENV)!!, JavaHttpClient().debug())

    override val model = GPT_IMAGE_2_5_FLARE
}
