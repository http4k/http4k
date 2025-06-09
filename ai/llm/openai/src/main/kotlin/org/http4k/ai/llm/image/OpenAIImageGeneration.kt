package org.http4k.ai.llm.image

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.mapFailure
import org.http4k.ai.llm.LLMError.Http
import org.http4k.ai.llm.OpenAIApi
import org.http4k.ai.llm.OpenAICompatibleClient
import org.http4k.ai.llm.model.Resource
import org.http4k.connect.openai.action.Size
import org.http4k.connect.openai.generateImage
import org.http4k.core.HttpHandler
import org.http4k.core.Response

/**
 * OpenAI ImageGeneration interface for using with the real OpenAI API endpoints
 */
fun ImageGeneration.Companion.OpenAI(apiKey: OpenAIApi.ApiKey, http: HttpHandler, org: OpenAIApi.Org? = null) =
    OpenAI(OpenAIApi(apiKey, http, org))

/**
 * ImageGeneration interface for any OpenAI-compatible client
 */
fun ImageGeneration.Companion.OpenAI(openAICompatibleClient: OpenAICompatibleClient) = object : ImageGeneration {
    private val client = openAICompatibleClient()

    override fun invoke(request: ImageRequest) = client.generateImage(
        request.prompt.value,
        request.size?.let { Size.valueOf(it.value) } ?: Size.`1024x1024`,
        when (request.responseFormat) {
            ImageResponseFormat.url -> org.http4k.connect.openai.action.ImageResponseFormat.url
            ImageResponseFormat.base64 -> org.http4k.connect.openai.action.ImageResponseFormat.b64_json
        },
        request.quantity,
        null,
        model = request.model
    ).map {
        ImageResponse(
            it.data.map {
                when (it.url) {
                    null -> Resource.Binary(it.b64_json!!, request.mimeType)
                    else -> Resource.Ref(it.url!!, request.mimeType)
                }
            }
        )
    }
        .mapFailure { Http(Response(it.status).body(it.message ?: "")) }
}
