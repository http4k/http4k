package org.http4k.security.oauth.server

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.lens.LensFailure
import org.http4k.security.ResponseType.Code

class AuthoriseRequestErrorRender(private val authoriseRequestValidator: AuthoriseRequestValidator,
                                  private val fallBack: JsonResponseErrorRenderer,
                                  private val documentationUri: String? = null) {

    fun errorFor(request: Request, error: OAuthError): Response {
        val clientId = extractValueOrNull(request) { OAuthServer.clientIdQueryParameter(it) }
        val redirectUri = extractValueOrNull(request) { OAuthServer.redirectUriQueryParameter(it) }
        val responseMode = extractValueOrNull(request) { OAuthServer.responseMode(it) }
        val responseType = extractValueOrNull(request) { OAuthServer.responseType(it) } ?: Code
        val state = extractValueOrNull(request) { OAuthServer.state(it) }
        return if (
            clientId == null ||
            redirectUri == null ||
            !authoriseRequestValidator.isValidClientAndRedirectUriInCaseOfError(request, clientId, redirectUri)) {
            fallBack.response(error)
        } else {
            val responseRender = ResponseRender.forAuthRequest(responseMode, responseType, redirectUri)
                .withState(state)
                .addParameter("error", error.rfcError.rfcValue)
                .addParameter("error_description", error.description)
            if (documentationUri != null) {
                responseRender.addParameter("error_uri", documentationUri).complete()
            } else {
                responseRender.complete()
            }
        }
    }

    private fun <T> extractValueOrNull(request: Request, callback: (Request) -> T?): T? {
        return try {
            callback(request)
        } catch (e: LensFailure) {
            null
        }
    }

}
