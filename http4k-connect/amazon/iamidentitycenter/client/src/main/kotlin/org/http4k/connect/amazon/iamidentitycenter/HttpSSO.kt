package org.http4k.connect.amazon.iamidentitycenter

import org.http4k.client.JavaHttpClient
import org.http4k.connect.amazon.core.model.Region
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters

/**
 * Standard HTTP implementation of SSO
 */
fun SSO.Companion.Http(
    region: Region,
    http: HttpHandler = JavaHttpClient(),
) = object : SSO {

    private val routedHttp = ClientFilters.SetHostFrom(Uri.of("https://portal.sso.$region.amazonaws.com"))
        .then(ClientFilters.SetXForwardedHost())
        .then(http)

    override fun <R : Any> invoke(action: SSOAction<R>) = action.toResult(routedHttp(action.toRequest()))
}
