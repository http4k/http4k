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
                    errorRenderer.render(InvalidClient)
                } else if (!clientValidator.validateRedirection(authorizationRequest.client, authorizationRequest.redirectUri)) {
                    errorRenderer.render(InvalidRedirect)
                } else {
                    next(it)
                }
            }
}

private object InvalidClient : OAuthError {
    override val rfcError = RfcError.InvalidClient
    override val description = "The specified client id is invalid"
}

private object InvalidRedirect : OAuthError {
    override val rfcError = RfcError.InvalidClient
    override val description: String = "The specified redirect uri is not registered"
}