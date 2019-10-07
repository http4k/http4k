package org.http4k.security.oauth.server

import com.natpryce.get
import com.natpryce.map
import com.natpryce.mapFailure
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.security.ResponseType

class ClientValidationFilter(private val clientValidator: ClientValidator,
                             private val errorRenderer: ErrorRenderer,
                             private val extractor: AuthRequestExtractor) : Filter {

    override fun invoke(next: HttpHandler): HttpHandler =
        {
            if (!validResponseTypes.contains(it.query("response_type"))) {
                errorRenderer.response(UnsupportedResponseType(it.query("response_type").orEmpty()))
            } else {
                extractor.extract(it).map { authorizationRequest ->
                    if (!clientValidator.validateClientId(it, authorizationRequest.client)) {
                        errorRenderer.response(InvalidClientId)
                    } else if (!clientValidator.validateRedirection(it, authorizationRequest.client, authorizationRequest.redirectUri)) {
                        errorRenderer.response(InvalidRedirectUri)
                    } else if(!clientValidator.validateScopes(it, authorizationRequest.client, authorizationRequest.scopes)){
                        errorRenderer.response(InvalidScopes)
                    } else {
                        next(it)
                    }
                }.mapFailure(errorRenderer::response).get()
            }
        }

    companion object {
        val validResponseTypes = ResponseType.values().map { it.queryParameterValue }
    }
}