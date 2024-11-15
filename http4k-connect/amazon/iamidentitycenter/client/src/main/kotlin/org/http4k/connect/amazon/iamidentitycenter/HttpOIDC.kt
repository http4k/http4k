package org.http4k.connect.amazon.iamidentitycenter

import org.http4k.client.JavaHttpClient
import org.http4k.connect.amazon.core.model.Region
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.ClientFilters.SetXForwardedHost

/**
 * Standard HTTP implementation of OIDC
 */
fun OIDC.Companion.Http(
    region: Region,
    http: HttpHandler = JavaHttpClient(),
) = object : OIDC {
    private val routedHttp = ClientFilters.SetHostFrom(Uri.of("https://oidc.$region.amazonaws.com"))
        .then(SetXForwardedHost())
        .then(http)

    override fun <R : Any> invoke(action: OIDCAction<R>) = action.toResult(routedHttp(action.toRequest()))
}

