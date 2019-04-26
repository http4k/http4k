package org.http4k.security.oauth.server

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then
import org.http4k.filter.ServerFilters

class ClientValidationFilter(private val clientValidator: ClientValidator,
                             private val errorRenderer: ErrorRenderer) : Filter {

    override fun invoke(next: HttpHandler): HttpHandler =
        ServerFilters.CatchLensFailure
            .then {
                val authorizationRequest = it.authorizationRequest()
                if(!clientValidator.validateClientId(authorizationRequest.client)) {
                    errorRenderer.render(Response(Status.BAD_REQUEST), "invalid_client", "The specified client id is invalid")
                } else if (!clientValidator.validateRedirection(authorizationRequest.client, authorizationRequest.redirectUri)) {
                    errorRenderer.render(Response(Status.BAD_REQUEST), "invalid_client", "The specified redirect uri is not registered")
                } else {
                    next(it)
                }
            }
}