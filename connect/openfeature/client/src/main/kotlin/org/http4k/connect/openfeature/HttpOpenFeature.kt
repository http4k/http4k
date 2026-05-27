package org.http4k.connect.openfeature

import dev.forkhandles.result4k.Result
import org.http4k.client.JavaHttpClient
import org.http4k.connect.RemoteFailure
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters.BearerAuth
import org.http4k.filter.ClientFilters.SetBaseUriFrom

fun OpenFeature.Companion.Http(
    baseUri: Uri,
    token: () -> OpenFeatureToken? = { null },
    http: HttpHandler = JavaHttpClient()
) = object : OpenFeature {
    private val routedHttp = SetBaseUriFrom(baseUri).then(http)

    override fun <R : Any> invoke(action: OpenFeatureAction<R>): Result<R, RemoteFailure> {
        val handler = token()
            ?.let { BearerAuth { it.value }.then(routedHttp) }
            ?: routedHttp
        return action.toResult(handler(action.toRequest()))
    }
}
