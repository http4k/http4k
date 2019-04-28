package org.http4k.security.oauth.server

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.then
import org.http4k.filter.ServerFilters
import org.http4k.security.ResponseType

class ClientValidationFilter(private val clientValidator: ClientValidator,
                             private val errorRenderer: ErrorRenderer) : Filter {

    override fun invoke(next: HttpHandler): HttpHandler =
        ServerFilters.CatchLensFailure
            .then {
                if (!validResponseTypes.contains(it.query("response_type"))) {
                    return@then errorRenderer.response(UnsupportedResponseType(it.query("response_type").orEmpty()))
                }
                val authorizationRequest = it.authorizationRequest()
                if(!clientValidator.validateClientId(authorizationRequest.client)) {
                    errorRenderer.response(InvalidClientId)
                } else if (!clientValidator.validateRedirection(authorizationRequest.client, authorizationRequest.redirectUri)) {
                    errorRenderer.response(InvalidRedirectUri)
                } else {
                    next(it)
                }
            }

    companion object {
        val validResponseTypes = ResponseType.values().map { it.queryParameterValue }
    }
}