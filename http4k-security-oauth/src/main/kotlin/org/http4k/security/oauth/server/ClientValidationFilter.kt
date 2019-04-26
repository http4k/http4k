package org.http4k.security.oauth.server

import org.http4k.core.*
import org.http4k.filter.ServerFilters
import org.http4k.format.AutoMarshallingJson
import org.http4k.lens.Header
import kotlin.with

class ClientValidationFilter(private val clientValidator: ClientValidator,
                             private val json: AutoMarshallingJson,
                             private val documentationUri: String = "") : Filter {

    override fun invoke(next: HttpHandler): HttpHandler =
        ServerFilters.CatchLensFailure
            .then {
                val authorizationRequest = it.authorizationRequest()
                if(!clientValidator.validateClientId(authorizationRequest.client)) {
                    Response(Status.BAD_REQUEST).withError("invalid_client", "The specified client id is invalid")
                } else if (!clientValidator.validateRedirection(authorizationRequest.client, authorizationRequest.redirectUri)) {
                    Response(Status.BAD_REQUEST).withError("invalid_client", "The specified redirect uri is not registered")
                } else {
                    next(it)
                }
            }

    private fun Response.withError(error: String, errorDescription: String) =
            with(Header.CONTENT_TYPE of ContentType.APPLICATION_JSON)
                    .body(json.asJsonString(ErrorResponse(error, errorDescription, documentationUri)))
}