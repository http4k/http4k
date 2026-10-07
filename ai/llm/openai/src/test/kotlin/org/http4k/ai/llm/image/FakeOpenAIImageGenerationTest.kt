package org.http4k.ai.llm.image

import org.http4k.ai.model.ApiKey
import org.http4k.connect.openai.FakeOpenAI
import org.http4k.connect.openai.OpenAIModels.GPT_IMAGE_2_5_FLARE

class FakeOpenAIImageGenerationTest : ImageGenerationContract {
    override val imageGeneration = ImageGeneration.OpenAI(ApiKey.of("asd"), FakeOpenAI())
    override val model = GPT_IMAGE_2_5_FLARE
}
