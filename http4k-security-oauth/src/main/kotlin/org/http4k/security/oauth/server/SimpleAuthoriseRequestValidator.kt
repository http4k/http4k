package org.http4k.security.oauth.server

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.http4k.core.Request
import org.http4k.core.Uri

class SimpleAuthoriseRequestValidator(private val clientValidator: ClientValidator) : AuthoriseRequestValidator {

    override fun isValidClientAndRedirectUriInCaseOfError(request: Request, clientId: ClientId, redirectUri: Uri): Boolean = clientValidator.validateRedirection(request, clientId, redirectUri)

    override fun validate(request: Request, authorizationRequest: AuthRequest): Result<Request, OAuthError> {
        return if (!clientValidator.validateClientId(request, authorizationRequest.client)) {
            Failure(InvalidClientId)
        } else if (!clientValidator.validateRedirection(request, authorizationRequest.client, authorizationRequest.redirectUri!!)) {
            Failure(InvalidRedirectUri)
        } else if (!clientValidator.validateScopes(request, authorizationRequest.client, authorizationRequest.scopes)) {
            Failure(InvalidScopes)
        } else {
            Success(request)
        }
    }

}
