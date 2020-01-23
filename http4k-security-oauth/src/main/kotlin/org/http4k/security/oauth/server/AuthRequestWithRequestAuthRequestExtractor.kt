package org.http4k.security.oauth.server

import com.natpryce.Failure
import com.natpryce.Result
import com.natpryce.Success
import com.natpryce.flatMap
import com.natpryce.mapFailure
import com.natpryce.onFailure
import org.http4k.core.Request
import org.http4k.security.ResponseType.Code
import org.http4k.security.oauth.server.request.RequestJWTValidator
import org.http4k.security.oauth.server.request.RequestObject
import org.http4k.security.oauth.server.request.RequestObjectExtractor

class AuthRequestWithRequestAuthRequestExtractor(private val requestJWTValidator: RequestJWTValidator) : AuthRequestExtractor {

    override fun extract(request: Request): Result<AuthRequest, InvalidAuthorizationRequest> {

        return AuthRequestFromQueryParameters.extract(request).flatMap { authRequest ->
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
    }

    private fun combineAuthRequestAndRequestObject(authRequest: AuthRequest, requestObject: RequestObject): Result<AuthRequest, InvalidAuthorizationRequest> {
        if (requestObject.client != null && authRequest.client != requestObject.client) {
            return Failure(InvalidAuthorizationRequest("'client_id' is invalid"))
        }
        return Success(authRequest.copy(
            requestObject = requestObject,
            redirectUri = nonNullValueIfExistsOrErrorIfNotEqual(authRequest.redirectUri, requestObject.redirectUri).onFailure { return it },
            state = nonNullValueIfExistsOrErrorIfNotEqual(authRequest.state, requestObject.state).onFailure { return it },
            nonce = nonNullValueIfExistsOrErrorIfNotEqual(authRequest.nonce, requestObject.nonce).onFailure { return it },
            responseType = nonNullValueIfExistsOrErrorIfNotEqual(authRequest.responseType, requestObject.responseType).onFailure { return it } ?: Code,
            responseMode = nonNullValueIfExistsOrErrorIfNotEqual(authRequest.responseMode, requestObject.responseMode).onFailure { return it },
            scopes = nonNullValueIfExistsOrErrorIfNotEqual(authRequest.scopes, requestObject.scopes())
            { scopes1, scopes2 -> scopes1.toSet() == scopes2.toSet() }
                .onFailure { return it } ?: emptyList()
        ))
    }

    private fun <T> nonNullValueIfExistsOrErrorIfNotEqual(authRequestValue: T?,
                                                          requestObjectValue: T?,
                                                          equalityOperator: (t1: T, t2: T) -> Boolean = { t1, t2 -> t1 == t2 }): Result<T?, InvalidAuthorizationRequest> {
        if (authRequestValue != null && requestObjectValue != null && !equalityOperator(authRequestValue, requestObjectValue)) {
            return Failure(InvalidAuthorizationRequest("request object is invalid"))
        }
        return Success(authRequestValue ?: requestObjectValue)
    }

}
