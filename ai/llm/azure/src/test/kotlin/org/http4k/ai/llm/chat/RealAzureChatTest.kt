package org.http4k.ai.llm.chat

import org.http4k.ai.llm.AzureRegion
import org.http4k.ai.llm.AzureResource
import org.http4k.ai.model.ApiKey
import org.http4k.client.JavaHttpClient
import org.http4k.config.Environment.Companion.ENV
import org.http4k.config.EnvironmentKey
import org.http4k.connect.openai.OpenAIModels.GPT3_5
import org.http4k.filter.debug
import org.http4k.lens.value
import org.http4k.util.PortBasedTest
import org.junit.jupiter.api.Assumptions.assumeTrue

class RealAzureChatTest : ChatContract, StreamingChatContract, PortBasedTest {

    val apiKey = EnvironmentKey.value(ApiKey).optional("AZURE_AI_API_KEY")
    val resource = EnvironmentKey.value(AzureResource).required("AZURE_AI_RESOURCE")
    val region = EnvironmentKey.value(AzureRegion).required("AZURE_AI_REGION")

    init {
        assumeTrue(apiKey(ENV) != null, "No API Key set - skipping")
    }

    override val chat = Chat.Azure(
        apiKey(ENV)!!,
        resource(ENV),
        region(ENV),
        JavaHttpClient().debug()
    )

    override val streamingChat = StreamingChat.Azure(
        apiKey(ENV)!!,
        resource(ENV),
        region(ENV),
        JavaHttpClient().debug()
    )

    override val model = GPT3_5
}
