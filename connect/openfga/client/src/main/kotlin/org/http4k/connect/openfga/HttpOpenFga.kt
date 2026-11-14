package org.http4k.connect.openfga

import org.http4k.client.JavaHttpClient
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters.SetBaseUriFrom

fun OpenFga.Companion.Http(
    baseUri: Uri,
    http: HttpHandler = JavaHttpClient()
) = object : OpenFga {
    private val routedHttp = SetBaseUriFrom(baseUri).then(http)

    override fun <R : Any> invoke(action: OpenFgaAction<R>) = action.toResult(routedHttp(action.toRequest()))
}
