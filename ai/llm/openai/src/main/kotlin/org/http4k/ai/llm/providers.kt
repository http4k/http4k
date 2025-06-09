package org.http4k.ai.llm

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue
import org.http4k.client.JavaHttpClient
import org.http4k.connect.openai.Http
import org.http4k.connect.openai.OpenAI
import org.http4k.connect.openai.OpenAIAction
import org.http4k.connect.openai.OpenAIOrg
import org.http4k.connect.openai.OpenAIToken
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters.BearerAuth
import org.http4k.filter.ClientFilters.SetBaseUriFrom


/**
 * Official OpenAI API integration
 */
class OpenAIApi(
    private val apiKey: ApiKey,
    private val http: HttpHandler = JavaHttpClient(),
    private val org: Org? = null
) : OpenAICompatibleClient {
    override fun invoke() = OpenAI.Http(OpenAIToken.of(apiKey.value), http, org?.let { OpenAIOrg.of(it.value) })

    class ApiKey private constructor(value: String) : StringValue(value) {
        companion object : NonBlankStringValueFactory<ApiKey>(::ApiKey)
    }

    class Org private constructor(value: String) : StringValue(value) {
        companion object : NonBlankStringValueFactory<Org>(::Org)
    }
}

/**
 * GitHubModels via Azure integration
 */
class GitHubModels(private val apiKey: ApiKey, private val http: HttpHandler = JavaHttpClient()) :
    OpenAICompatibleClient {
    override fun invoke() = object : OpenAI {
        private val routedHttp = SetBaseUriFrom(Uri.of("https://models.inference.ai.azure.com"))
            .then(BearerAuth(apiKey.value))
            .then(http)

        override fun <R> invoke(action: OpenAIAction<R>) = action.toResult(routedHttp(action.toRequest()))
    }

    class ApiKey private constructor(value: String) : StringValue(value) {
        companion object : NonBlankStringValueFactory<ApiKey>(::ApiKey)
    }
}

/**
 * Azure integration
 */
class Azure(
    private val apiKey: ApiKey,
    private val resource: Resource,
    private val region: Region,
    private val http: HttpHandler = JavaHttpClient()
) : OpenAICompatibleClient {
    override fun invoke() = object : OpenAI {
        private val routedHttp = SetBaseUriFrom(Uri.of("https://$resource.$region.models.ai.azure.com"))
            .then(BearerAuth(apiKey.value))
            .then(http)

        override fun <R> invoke(action: OpenAIAction<R>) = action.toResult(routedHttp(action.toRequest()))
    }

    class ApiKey private constructor(value: String) : StringValue(value) {
        companion object : NonBlankStringValueFactory<ApiKey>(::ApiKey)
    }

    class Region private constructor(value: String) : StringValue(value) {
        companion object : NonBlankStringValueFactory<Region>(::Region)
    }

    class Resource private constructor(value: String) : StringValue(value) {
        companion object : NonBlankStringValueFactory<Resource>(::Resource)
    }
}
