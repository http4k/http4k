package org.http4k.connect.azure

import org.http4k.client.JavaHttpClient
import org.http4k.config.Environment.Companion.ENV
import org.http4k.config.EnvironmentKey
import org.http4k.filter.debug
import org.http4k.lens.value
import org.http4k.util.PortBasedTest
import org.junit.jupiter.api.Assumptions.assumeTrue

class RealAzureAITest : AzureAIContract, PortBasedTest {
    val apiKey = EnvironmentKey.value(AzureAIApiKey).optional("AZURE_AI_API_KEY")
    val resource = EnvironmentKey.value(AzureResource).required("AZURE_AI_RESOURCE")
    val region = EnvironmentKey.value(Region).required("AZURE_AI_REGION")

    init {
        assumeTrue(apiKey(ENV) != null, "No API Key set - skipping")
    }

    override val azureAi: AzureAI = AzureAI.Http(
        resource(ENV),
        region(ENV),
        apiKey(ENV)!!,
        JavaHttpClient().debug()
    )
}

