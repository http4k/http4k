package org.http4k.security.oauth.server

import com.natpryce.Failure
import com.natpryce.Result
import com.natpryce.Success
import org.http4k.core.Request

class SimpleAuthoriseRequestValidator(private val clientValidator: ClientValidator) : AuthoriseRequestValidator {

    override fun validate(request: Request, authorizationRequest: AuthRequest): Result<AuthRequest, OAuthError> {
        return if (!clientValidator.validateClientId(request, authorizationRequest.client)) {
            Failure(InvalidClientId)
        } else if (!clientValidator.validateRedirection(request, authorizationRequest.client, authorizationRequest.redirectUri)) {
            Failure(InvalidRedirectUri)
        } else if (!clientValidator.validateScopes(request, authorizationRequest.client, authorizationRequest.scopes)) {
            Failure(InvalidScopes)
        } else {
            Success(authorizationRequest)
        }
    }

}