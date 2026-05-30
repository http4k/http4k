package org.http4k.security.oauth.server

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.http4k.core.Request
import org.http4k.core.Uri

interface AuthoriseRequestValidator {

    fun isValidClientAndRedirectUriInCaseOfError(request: Request, clientId: ClientId, redirectUri: Uri): Boolean

    fun validate(request: Request, authorizationRequest: AuthRequest): Result<Request, OAuthError>

    companion object {
        val AlwaysValid: AuthoriseRequestValidator = object : AuthoriseRequestValidator {
            override fun isValidClientAndRedirectUriInCaseOfError(
                request: Request,
                clientId: ClientId,
                redirectUri: Uri
            ): Boolean = true

            override fun validate(request: Request, authorizationRequest: AuthRequest): Result<Request, OAuthError> =
                Success(request)
        }
    }
}

class MustHaveRedirectUri(private val delegate: AuthoriseRequestValidator) : AuthoriseRequestValidator by delegate {

    override fun validate(request: Request, authorizationRequest: AuthRequest): Result<Request, OAuthError> {
        if (authorizationRequest.redirectUri == null) {
            return Failure(InvalidAuthorizationRequest("query 'redirect_uri' is required"))
        }
        return delegate.validate(request, authorizationRequest)
    }
}

class RequirePkce(private val delegate: AuthoriseRequestValidator) : AuthoriseRequestValidator by delegate {

    override fun validate(request: Request, authorizationRequest: AuthRequest): Result<Request, OAuthError> =
        when (authorizationRequest.codeChallenge) {
            null -> Failure(InvalidAuthorizationRequest("query 'code_challenge' is required"))
            else -> delegate.validate(request, authorizationRequest)
        }
}
