package org.http4k.connect.typesafe

import org.http4k.ai.model.ApiKey
import org.http4k.client.JavaHttpClient
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters

fun TypeSafe.Companion.Http(
    apiKey: ApiKey,
    http: HttpHandler = JavaHttpClient()
) = object : TypeSafe {
    private val routedHttp = ClientFilters.SetHostFrom(Uri.of("https://api.typesafe.ai"))
        .then(ClientFilters.BearerAuth(apiKey.value))
        .then(http)

    override fun <R> invoke(action: TypeSafeAction<R>) = action.toResult(routedHttp(action.toRequest()))
}
