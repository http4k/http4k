package org.http4k.security.oauth.server

import org.http4k.core.Request
import org.http4k.security.ResponseType.Code
import org.http4k.security.oauth.server.request.RequestJWTValidator
import org.http4k.security.oauth.server.request.RequestObject
import org.http4k.security.oauth.server.request.RequestObjectExtractor
import org.http4k.util.Failure
import org.http4k.util.Success
import org.http4k.util.flatMap
import org.http4k.util.mapFailure
import org.http4k.util.onFailure

class AuthRequestWithRequestAuthRequestExtractor(private val requestJWTValidator: RequestJWTValidator) : AuthRequestExtractor {

    override fun extract(request: Request) =
        AuthRequestFromQueryParameters.extract(request).flatMap { authRequest ->
            val requestJwtContainer = authRequest.request
            if (requestJwtContainer != null) {
                val requestJwtValidationError = requestJWTValidator.validate(authRequest.client, requestJwtContainer)
                if (requestJwtValidationError != null) {
                    Failure(requestJwtValidationError)
                } else {
                    RequestObjectExtractor.extractRequestObjectFromJwt(requestJwtContainer.value)
                        .mapFailure { InvalidAuthorizationRequest("Query 'request' is invalid") }
                        .flatMap { requestObject -> combineAuthRequestAndRequestObject(authRequest, requestObject) }
                }
            } else {
                Success(authRequest)
            }
        }

    private fun combineAuthRequestAndRequestObject(authRequest: AuthRequest, requestObject: RequestObject) =
        if (requestObject.client != null && authRequest.client != requestObject.client) {
            Failure(InvalidAuthorizationRequest("'client_id' is invalid"))
        } else
            Success(authRequest.copy(
                requestObject = requestObject,
                redirectUri = nonNullValueIfExistsOrErrorIfNotEqual(authRequest.redirectUri, requestObject.redirectUri).onFailure { return it },
                state = nonNullValueIfExistsOrErrorIfNotEqual(authRequest.state, requestObject.state).onFailure { return it },
                nonce = nonNullValueIfExistsOrErrorIfNotEqual(authRequest.nonce, requestObject.nonce).onFailure { return it },
                responseType = nonNullValueIfExistsOrErrorIfNotEqual(authRequest.responseType, requestObject.responseType).onFailure { return it }
                    ?: Code,
                responseMode = nonNullValueIfExistsOrErrorIfNotEqual(authRequest.responseMode, requestObject.responseMode).onFailure { return it },
                scopes = nonEmptyScopeIfExistsOrErrorIfNotEqual(authRequest.scopes, requestObject.scope)
                    .onFailure { return it }
            ))

    private fun <T> nonNullValueIfExistsOrErrorIfNotEqual(authRequestValue: T?, requestObjectValue: T?) =
        if (authRequestValue != null && requestObjectValue != null && authRequestValue != requestObjectValue) {
            Failure(InvalidAuthorizationRequest("request object is invalid"))
        } else
            Success(authRequestValue ?: requestObjectValue)

    private fun nonEmptyScopeIfExistsOrErrorIfNotEqual(authRequestValue: List<String>,
                                                       requestObjectValue: List<String>) =
        if (authRequestValue.isNotEmpty() && requestObjectValue.isNotEmpty() && authRequestValue.toSet() != requestObjectValue.toSet()) {
            Failure(InvalidAuthorizationRequest("request object is invalid"))
        } else
            Success(if (authRequestValue.isNotEmpty()) authRequestValue else requestObjectValue)

}
