package org.http4k.ai.llm

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue
import org.http4k.ai.model.ApiKey
import org.http4k.client.JavaHttpClient
import org.http4k.connect.openai.Http
import org.http4k.connect.openai.OpenAI
import org.http4k.connect.openai.OpenAIOrg
import org.http4k.connect.openai.OpenAIToken
import org.http4k.core.HttpHandler


/**
 * Official OpenAI API integration
 */
class OpenAIApi(
    private val apiKey: ApiKey,
    private val http: HttpHandler = JavaHttpClient(),
    private val org: Org? = null
) : OpenAICompatibleClient {
    override fun invoke() = OpenAI.Http(OpenAIToken.of(apiKey.value), http, org?.let { OpenAIOrg.of(it.value) })

    class Org private constructor(value: String) : StringValue(value) {
        companion object : NonBlankStringValueFactory<Org>(::Org)
    }
}
