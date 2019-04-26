package org.http4k.security.oauth.server

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.then
import org.http4k.filter.ServerFilters

class ClientValidationFilter(private val clientValidator: ClientValidator,
                             private val errorRenderer: ErrorRenderer) : Filter {

    override fun invoke(next: HttpHandler): HttpHandler =
        ServerFilters.CatchLensFailure
            .then {
                val authorizationRequest = it.authorizationRequest()
                if(!clientValidator.validateClientId(authorizationRequest.client)) {
                    errorRenderer.render(InvalidClientId)
                } else if (!clientValidator.validateRedirection(authorizationRequest.client, authorizationRequest.redirectUri)) {
                    errorRenderer.render(InvalidRedirectUri)
                } else {
                    next(it)
                }
            }
}