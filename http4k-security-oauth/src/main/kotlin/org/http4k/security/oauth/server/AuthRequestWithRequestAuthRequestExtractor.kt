package org.http4k.security.oauth.server

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.mapFailure
import dev.forkhandles.result4k.onFailure
import org.http4k.core.Request
import org.http4k.security.ResponseType.Code
import org.http4k.security.oauth.server.request.RequestJWTValidator
import org.http4k.security.oauth.server.request.RequestObject
import org.http4k.security.oauth.server.request.RequestObjectExtractor

class AuthRequestWithRequestAuthRequestExtractor(
    private val requestJWTValidator: RequestJWTValidator,
    private val combineAuthRequestRequestStrategy: CombineAuthRequestRequestStrategy
) : AuthRequestExtractor {

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
            responseType = nonNullValueIfExistsOrErrorIfNotEqual(authRequest.responseType, requestObject.responseType).onFailure { return it }
                ?: Code,
            responseMode = nonNullValueIfExistsOrErrorIfNotEqual(authRequest.responseMode, requestObject.responseMode).onFailure { return it },
            scopes = nonEmptyScopeIfExistsOrErrorIfNotEqual(authRequest.scopes, requestObject.scope)
                .onFailure { return it }
        ))
    }

    private fun <T> nonNullValueIfExistsOrErrorIfNotEqual(
        authRequestValue: T?,
        requestObjectValue: T?
    ): Result<T?, InvalidAuthorizationRequest> {
        if (authRequestValue != null && requestObjectValue != null && authRequestValue != requestObjectValue) {
            return Failure(InvalidAuthorizationRequest("request object is invalid"))
        }
        return Success(combineAuthRequestRequestStrategy.combine(authRequestValue, requestObjectValue))
    }

    private fun nonEmptyScopeIfExistsOrErrorIfNotEqual(
        authRequestValue: List<String>,
        requestObjectValue: List<String>
    ): Result<List<String>, InvalidAuthorizationRequest> {
        if (authRequestValue.isNotEmpty() && requestObjectValue.isNotEmpty() && authRequestValue.toSet() != requestObjectValue.toSet()) {
            return Failure(InvalidAuthorizationRequest("request object is invalid"))
        }
        return Success(combineAuthRequestRequestStrategy.combine(authRequestValue, requestObjectValue))
    }

    sealed class CombineAuthRequestRequestStrategy {

        object Combine : CombineAuthRequestRequestStrategy() {
            override fun <T> combine(authRequestValue: T?, requestObjectValue: T?): T? =
                authRequestValue ?: requestObjectValue

            override fun <T> combine(authRequestValue: List<T>, requestObjectValue: List<T>): List<T> =
                if (authRequestValue.isNotEmpty()) authRequestValue else requestObjectValue
        }

        object AuthRequestOnly : CombineAuthRequestRequestStrategy() {
            override fun <T> combine(authRequestValue: T?, requestObjectValue: T?): T? =
                authRequestValue

            override fun <T> combine(authRequestValue: List<T>, requestObjectValue: List<T>): List<T> =
                authRequestValue
        }

        object RequestObjectOnly : CombineAuthRequestRequestStrategy() {
            override fun <T> combine(authRequestValue: T?, requestObjectValue: T?): T? =
                requestObjectValue

            override fun <T> combine(authRequestValue: List<T>, requestObjectValue: List<T>): List<T> =
                requestObjectValue
        }

        abstract fun <T> combine(authRequestValue: T?, requestObjectValue: T?): T?
        abstract fun <T> combine(authRequestValue: List<T>, requestObjectValue: List<T>): List<T>
    }
}
