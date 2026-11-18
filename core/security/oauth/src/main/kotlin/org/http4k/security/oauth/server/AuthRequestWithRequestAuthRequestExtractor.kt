package org.http4k.security.oauth.server

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.mapFailure
import org.http4k.core.Request
import org.http4k.security.ResponseType.Code
import org.http4k.security.oauth.server.request.RequestJWTValidator
import org.http4k.security.oauth.server.request.RequestObject
import org.http4k.security.oauth.server.request.RequestObjectExtractor.extractRequestObjectFromJwt

class AuthRequestWithRequestAuthRequestExtractor(
    private val requestJWTValidator: RequestJWTValidator,
    private val combineAuthRequestRequestStrategy: CombineAuthRequestRequestStrategy
) : AuthRequestExtractor {

    override fun extract(request: Request) =
        AuthRequestFromQueryParameters.extract(request).flatMap { authRequest ->
            when (val requestJwtContainer = authRequest.request) {
                null -> Success(authRequest)

                else -> requestJWTValidator.validate(authRequest.client, requestJwtContainer)?.let { Failure(it) }
                    ?: extractRequestObjectFromJwt(requestJwtContainer.value)
                        .mapFailure { InvalidAuthorizationRequest("Query 'request' is invalid") }
                        .flatMap { requestObject -> authRequest.combineWith(requestObject) }
            }
        }

    private fun AuthRequest.combineWith(requestObject: RequestObject) = when {
        requestObject.client != null && client != requestObject.client ->
            Failure(InvalidAuthorizationRequest("'client_id' is invalid"))

        isInConflictWith(requestObject) -> Failure(InvalidAuthorizationRequest("request object is invalid"))

        else -> with(combineAuthRequestRequestStrategy) {
            Success(
                copy(
                    requestObject = requestObject,
                    redirectUri = combine(redirectUri, requestObject.redirectUri),
                    state = combine(state, requestObject.state),
                    nonce = combine(nonce, requestObject.nonce),
                    responseType = combine(responseType, requestObject.responseType) ?: Code,
                    responseMode = combine(responseMode, requestObject.responseMode),
                    scopes = this.combine(scopes, requestObject.scope)
                )
            )
        }
    }

    private fun AuthRequest.isInConflictWith(requestObject: RequestObject) =
        conflicts(redirectUri, requestObject.redirectUri) ||
            conflicts(state, requestObject.state) ||
            conflicts(nonce, requestObject.nonce) ||
            conflicts(responseType, requestObject.responseType) ||
            conflicts(responseMode, requestObject.responseMode) ||
            (scopes.isNotEmpty() && requestObject.scope.isNotEmpty() && scopes.toSet() != requestObject.scope.toSet())

    private fun <T> conflicts(authRequestValue: T?, requestObjectValue: T?) =
        authRequestValue != null && requestObjectValue != null && authRequestValue != requestObjectValue

    sealed interface CombineAuthRequestRequestStrategy {

        data object Combine : CombineAuthRequestRequestStrategy {
            override fun <T> combine(authRequestValue: T?, requestObjectValue: T?): T? =
                authRequestValue ?: requestObjectValue

            override fun <T> combine(authRequestValue: List<T>, requestObjectValue: List<T>): List<T> =
                authRequestValue.ifEmpty { requestObjectValue }
        }

        data object AuthRequestOnly : CombineAuthRequestRequestStrategy {
            override fun <T> combine(authRequestValue: T?, requestObjectValue: T?): T? =
                authRequestValue

            override fun <T> combine(authRequestValue: List<T>, requestObjectValue: List<T>): List<T> =
                authRequestValue
        }

        data object RequestObjectOnly : CombineAuthRequestRequestStrategy {
            override fun <T> combine(authRequestValue: T?, requestObjectValue: T?): T? =
                requestObjectValue

            override fun <T> combine(authRequestValue: List<T>, requestObjectValue: List<T>): List<T> =
                requestObjectValue
        }

        fun <T> combine(authRequestValue: T?, requestObjectValue: T?): T?
        fun <T> combine(authRequestValue: List<T>, requestObjectValue: List<T>): List<T>
    }
}
