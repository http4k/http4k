package org.http4k.ai.llm.chat

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.mapFailure
import org.http4k.ai.llm.AzureClient
import org.http4k.ai.llm.AzureRegion
import org.http4k.ai.llm.AzureResource
import org.http4k.ai.llm.LLMError.Http
import org.http4k.ai.model.ApiKey
import org.http4k.client.JavaHttpClient
import org.http4k.core.HttpHandler
import org.http4k.core.Response

/**
 * Azure Streaming Chat interface
 */
fun StreamingChat.Companion.Azure(
    apiKey: ApiKey,
    resource: AzureResource,
    region: AzureRegion,
    http: HttpHandler = JavaHttpClient()
) = object : StreamingChat {
    private val client = AzureClient(apiKey, resource, region, http)()

    override fun invoke(request: ChatRequest) = client(request.toOpenAI(true))
        .map { it.map { it.toHttp4k() } }
        .mapFailure { Http(Response(it.status).body(it.message ?: "")) }
}
