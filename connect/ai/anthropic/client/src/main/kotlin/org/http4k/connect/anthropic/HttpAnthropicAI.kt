package org.http4k.connect.anthropic

import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.RequestFilters.SetHeader

fun AnthropicAI.Companion.Http(
    apiKey: AnthropicIApiKey,
    apiVersion: ApiVersion,
    http: HttpHandler,
) = object : AnthropicAI {
    private val routedHttp = ClientFilters.SetHostFrom(Uri.of("https://api.anthropic.com"))
        .then(SetHeader("x-api-key", apiKey.value))
        .then(SetHeader("anthropic-version", apiVersion.value.toString()))
        .then(http)

    override fun <R> invoke(action: AnthropicAIAction<R>) = action.toResult(routedHttp(action.toRequest()))
}
