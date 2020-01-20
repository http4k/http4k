package org.http4k.security.oauth.server

import com.natpryce.Result
import org.http4k.core.Request
import org.http4k.core.Uri

interface AuthoriseRequestValidator {

    fun isValidClientAndRedirectUriInCaseOfError(request: Request, clientId: ClientId, redirectUri: Uri): Boolean

    fun validate(request: Request, authorizationRequest: AuthRequest): Result<Request, OAuthError>

}

