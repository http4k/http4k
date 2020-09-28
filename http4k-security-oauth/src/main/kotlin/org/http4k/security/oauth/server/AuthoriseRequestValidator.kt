package org.http4k.security.oauth.server

import com.natpryce.Failure
import com.natpryce.Result
import org.http4k.core.Request
import org.http4k.core.Uri

interface AuthoriseRequestValidator {

    fun isValidClientAndRedirectUriInCaseOfError(request: Request, clientId: ClientId, redirectUri: Uri): Boolean

    fun validate(request: Request, authorizationRequest: AuthRequest): Result<Request, OAuthError>
}

class MustHaveRedirectUri(private val delegate: AuthoriseRequestValidator) : AuthoriseRequestValidator by delegate {

    override fun validate(request: Request, authorizationRequest: AuthRequest): Result<Request, OAuthError> {
        if (authorizationRequest.redirectUri == null) {
            return Failure(InvalidAuthorizationRequest("query 'redirect_uri' is required"))
        }
        return delegate.validate(request, authorizationRequest)
    }

}

