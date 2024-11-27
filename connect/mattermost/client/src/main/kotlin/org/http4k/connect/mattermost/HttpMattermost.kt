package org.http4k.connect.mattermost

import org.http4k.client.JavaHttpClient
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters

fun Mattermost.Companion.Http(
    baseUri: Uri,
    http: HttpHandler = JavaHttpClient(),
) = object : Mattermost {
    private val routedHttp = ClientFilters.SetBaseUriFrom(baseUri)
        .then(http)

    override fun <R> invoke(action: MattermostAction<R>) = action.toResult(routedHttp(action.toRequest()))
}
