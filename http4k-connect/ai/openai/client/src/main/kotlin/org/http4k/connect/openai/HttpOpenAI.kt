package org.http4k.connect.openai

import org.http4k.client.JavaHttpClient
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.NoOp
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ClientFilters.BearerAuth
import org.http4k.filter.ClientFilters.SetBaseUriFrom
import org.http4k.lens.Header
import org.http4k.lens.value

fun OpenAI.Companion.Http(
    token: OpenAIToken,
    http: HttpHandler = JavaHttpClient(),
    org: OpenAIOrg? = null
) = object : OpenAI {

    private val routedHttp = SetBaseUriFrom(Uri.of("https://api.openai.com"))
        .then(BearerAuth(token.value))
        .then(org?.let(::AddOrg) ?: Filter.NoOp)
        .then(http)

    override fun <R> invoke(action: OpenAIAction<R>) = action.toResult(routedHttp(action.toRequest()))
}

private fun AddOrg(org: OpenAIOrg) = Filter { next ->
    {
        next(it.with(Header.value(OpenAIOrg).optional("OpenAI-Organization") of org))
    }
}

