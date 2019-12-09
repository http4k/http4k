package org.http4k.security.oauth.server

import com.natpryce.Result
import org.http4k.core.Request

interface AuthoriseRequestValidator {

    fun validate(request: Request, authorizationRequest: AuthRequest): Result<Request, OAuthError>

}

