package org.http4k.connect.openfeature

import org.http4k.client.JavaHttpClient
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters.SetBaseUriFrom

fun OpenFeature.Companion.Http(
    baseUri: Uri,
    http: HttpHandler = JavaHttpClient()
) = object : OpenFeature {
    private val routedHttp = SetBaseUriFrom(baseUri).then(http)

    override fun <R : Any> invoke(action: OpenFeatureAction<R>) = action.toResult(routedHttp(action.toRequest()))
}
