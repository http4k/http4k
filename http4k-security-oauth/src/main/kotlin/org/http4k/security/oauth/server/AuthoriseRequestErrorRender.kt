package org.http4k.security.oauth.server

import dev.forkhandles.result4k.get
import dev.forkhandles.result4k.mapFailure
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.lens.LensFailure
import org.http4k.security.ResponseMode
import org.http4k.security.ResponseType
import org.http4k.security.ResponseType.Code
import org.http4k.security.State
import org.http4k.security.oauth.server.ResponseRender.Companion.forAuthRequest
import org.http4k.security.oauth.server.request.RequestJWTValidator
import org.http4k.security.oauth.server.request.RequestObjectExtractor.extractRequestJwtClaimsAsMap

class AuthoriseRequestErrorRender(
    private val authoriseRequestValidator: AuthoriseRequestValidator,
    private val requestJWTValidator: RequestJWTValidator,
    private val fallBack: JsonResponseErrorRenderer,
    private val documentationUri: String? = null
) {

    fun errorFor(request: Request, error: OAuthError): Response {
        val requestClientId = extractValueFromRequestOrNull(request) { OAuthServer.clientIdQueryParameter(it) }
        val requestJwt = extractValueFromRequestOrNull(request) { OAuthServer.request(it) }
        val isRequestJwtValid = requestJwt?.let { jwt ->
            requestClientId?.let { clientId -> requestJWTValidator.validate(clientId, jwt) == null }
        }
        val requestObjectMap = requestJwt
            ?.let { jwt ->
                isRequestJwtValid?.let { isValidJwt ->
                    if (isValidJwt) extractRequestJwtClaimsAsMap(jwt.value).mapFailure { mapOf<Any, Any>() }.get()
                    else null
                }
            }
        val requestJwtClientId = requestObjectMap?.get("client_id")?.let { id -> ClientId(id.toString()) }
        val requestRedirectUri = extractValue(request, requestObjectMap, "redirect_uri", { OAuthServer.redirectUriQueryParameter(it) }) { Uri.of(it) }
        val requestResponseMode = extractValue(request, requestObjectMap, "response_mode", { OAuthServer.responseMode(it) }, ResponseMode.Companion::fromQueryParameterValue)
        val requestResponseType = extractValue(request, requestObjectMap, "response_type", { OAuthServer.responseType(it) }, ResponseType.Companion::fromQueryParameterValue)
            ?: Code
        val requestState = extractValue(request, requestObjectMap, "state", { OAuthServer.state(it) }) { State(it) }
        return if (isUnsafeToRedirectBackToRedirectUri(isRequestJwtValid, requestClientId, requestJwtClientId, requestRedirectUri, request)) {
            fallBack.response(error)
        } else {
            forAuthRequest(requestResponseMode, requestResponseType, requestRedirectUri!!)
                .withState(requestState)
                .addParameter("error", error.rfcError.rfcValue)
                .addParameter("error_description", error.description)
                .withDocumentationUri(documentationUri)
                .complete()
        }
    }

    private fun isUnsafeToRedirectBackToRedirectUri(
        isRequestJwtValid: Boolean?,
        requestClientId: ClientId?,
        requestJwtClientId: ClientId?,
        requestRedirectUri: Uri?,
        request: Request
    ): Boolean {
        return isRequestJwtValid == false ||
            requestClientId == null ||
            (requestJwtClientId != null && requestClientId != requestJwtClientId) ||
            requestRedirectUri == null ||
            !authoriseRequestValidator.isValidClientAndRedirectUriInCaseOfError(request, requestClientId, requestRedirectUri)
    }

    private fun <T> extractValue(request: Request, requestObject: Map<*, *>?, key: String, requestCallback: (Request) -> T?, requestObjectCallback: (String) -> T): T? {
        val requestState = extractValueFromRequestOrNull(request, requestCallback)
        val requestJwtState = requestObject?.let { extractValueFromRequestObjectOrNull(key, it, requestObjectCallback) }
        if (requestJwtState != null && requestState != null && requestState != requestJwtState) return null
        return requestState ?: requestJwtState
    }

    private fun <T> extractValueFromRequestOrNull(request: Request, callback: (Request) -> T?): T? {
        return try {
            callback(request)
        } catch (e: LensFailure) {
            null
        }
    }

    private fun <T> extractValueFromRequestObjectOrNull(key: String, request: Map<*, *>, callback: (String) -> T): T? {
        return try {
            request[key]?.toString()?.let(callback)
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}
