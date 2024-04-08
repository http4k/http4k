package org.http4k.security

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.get
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.mapFailure
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.TEMPORARY_REDIRECT
import org.http4k.core.Uri
import org.http4k.security.OAuthCallbackError.AuthorizationCodeMissing
import org.http4k.security.OAuthCallbackError.InvalidCsrfToken
import org.http4k.security.OAuthCallbackError.InvalidNonce
import org.http4k.security.oauth.server.AuthorizationCode
import org.http4k.security.openid.IdToken
import org.http4k.security.openid.IdTokenConsumer

class OAuthCallback(
    private val oAuthPersistence: OAuthPersistence,
    private val idTokenConsumer: IdTokenConsumer,
    private val accessTokenFetcher: AccessTokenFetcher
) : HttpHandler {

    override fun invoke(request: Request) = request.callbackParameters()
        .flatMap { parameters -> validateCsrf(parameters, request, oAuthPersistence.retrieveCsrf(request)) }
        .flatMap { parameters -> validateNonce(parameters, oAuthPersistence.retrieveNonce(request)) }
        .flatMap { parameters -> consumeIdToken(parameters) }
        .flatMap { parameters -> accessTokenFetcher.fetch(parameters.code.value) }
        .flatMap { tokenDetails -> consumeIdToken(tokenDetails) }
        .map { tokenDetails ->
            oAuthPersistence.assignToken(
                request,
                redirectionResponse(request),
                tokenDetails.accessToken,
                tokenDetails.idToken
            )
        }.mapFailure { oAuthPersistence.authFailureResponse(it) }.get()

    private fun Request.callbackParameters() = authorizationCode().map {
        CallbackParameters(
            code = it,
            state = queryOrFragmentParameter("state")?.let(::CrossSiteRequestForgeryToken),
            idToken = queryOrFragmentParameter("id_token")?.let(::IdToken)
        )
    }

    private fun Request.authorizationCode() = queryOrFragmentParameter("code")?.let(::AuthorizationCode)
        ?.let(::Success) ?: Failure(AuthorizationCodeMissing(uri))

    private fun validateCsrf(
        parameters: CallbackParameters,
        request: Request,
        persistedToken: CrossSiteRequestForgeryToken?
    ) = request.queryOrFragmentParameter("state")?.let(::CrossSiteRequestForgeryToken)
        .let {
            if (it == persistedToken) Success(parameters)
            else Failure(InvalidCsrfToken(persistedToken?.value, it?.value))
        }

    private fun validateNonce(parameters: CallbackParameters, storedNonce: Nonce?) =
        parameters.idToken?.let { idToken ->
            val received = idTokenConsumer.nonceFromIdToken(idToken)
            if (received == storedNonce)
                Success(parameters) else Failure(InvalidNonce(storedNonce?.value, received?.value))
        } ?: Success(parameters)

    private fun consumeIdToken(parameters: CallbackParameters) =
        parameters.idToken?.let(idTokenConsumer::consumeFromAuthorizationResponse)?.map { parameters }
            ?: Success(parameters)

    private fun consumeIdToken(tokenDetails: AccessTokenDetails) =
        tokenDetails.idToken?.let(idTokenConsumer::consumeFromAccessTokenResponse)?.map { tokenDetails }
            ?: Success(tokenDetails)

    private fun redirectionResponse(request: Request) = Response(TEMPORARY_REDIRECT)
        .header("Location", oAuthPersistence.retrieveOriginalUri(request)?.toString() ?: "/")

    private fun Request.queryOrFragmentParameter(name: String) = query(name) ?: fragmentParameter(name)

    private data class CallbackParameters(
        val code: AuthorizationCode,
        val state: CrossSiteRequestForgeryToken?,
        val idToken: IdToken?
    )
}

sealed class OAuthCallbackError {
    data class AuthorizationCodeMissing(val callbackUri: Uri) : OAuthCallbackError()
    data class InvalidCsrfToken(val expected: String?, val received: String?) : OAuthCallbackError()
    data class InvalidNonce(val expected: String?, val received: String?) : OAuthCallbackError()
    data class InvalidAccessToken(val reason: String) : OAuthCallbackError()
    data class InvalidIdToken(val reason: String) : OAuthCallbackError()
    data class CouldNotFetchAccessToken(val status: Status, val reason: String) : OAuthCallbackError()
}
