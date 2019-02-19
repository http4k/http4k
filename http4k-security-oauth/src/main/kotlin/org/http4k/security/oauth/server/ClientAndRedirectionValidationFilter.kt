package org.http4k.security.oauth.server

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then
import org.http4k.filter.ServerFilters

class ClientAndRedirectionValidationFilter(
    private val validateClientAndRedirectionUri: ClientValidator
) : Filter {

    override fun invoke(next: HttpHandler): HttpHandler =
        ServerFilters.CatchLensFailure
            .then {
                val authorizationRequest = it.authorizationRequest()
                if (!validateClientAndRedirectionUri(authorizationRequest.client, authorizationRequest.redirectUri)) {
                    Response(Status.BAD_REQUEST.description("invalid 'client_id' and/or 'redirect_uri'"))
                } else {
                    next(it)
                }
            }
}