package org.http4k.security.oauth.server

import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.util.Failure
import org.http4k.util.Result

interface AuthoriseRequestValidator {

    fun isValidClientAndRedirectUriInCaseOfError(request: Request, clientId: ClientId, redirectUri: Uri): Boolean

    fun validate(request: Request, authorizationRequest: AuthRequest): Result<OAuthError, Request>

}

class MustHaveRedirectUri(private val delegate: AuthoriseRequestValidator) : AuthoriseRequestValidator by delegate {

    override fun validate(request: Request, authorizationRequest: AuthRequest): Result<OAuthError, Request> {
        if (authorizationRequest.redirectUri == null) {
            return Failure(InvalidAuthorizationRequest("query 'redirect_uri' is required"))
        }
        return delegate.validate(request, authorizationRequest)
    }

}

