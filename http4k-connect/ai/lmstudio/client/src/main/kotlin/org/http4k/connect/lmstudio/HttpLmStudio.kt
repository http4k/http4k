package org.http4k.connect.lmstudio

import org.http4k.client.JavaHttpClient
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters.SetBaseUriFrom

fun LmStudio.Companion.Http(
    http: HttpHandler = JavaHttpClient(),
    baseUri: Uri = Uri.of("http://localhost:1234")
) = object : LmStudio {
    private val routedHttp = SetBaseUriFrom(baseUri).then(http)

    override fun <R> invoke(action: LmStudioAction<R>) = action.toResult(routedHttp(action.toRequest()))
}
