package org.http4k.security.oauth.server

import com.natpryce.Result
import com.natpryce.Success
import org.http4k.core.Request

interface AuthRequestExtractor {
    fun extract(request: Request): Result<AuthRequest, InvalidAuthorizationRequest>
}

object BasicAuthRequestExtractor : AuthRequestExtractor {
    override fun extract(request: Request): Result<AuthRequest, InvalidAuthorizationRequest> =
        Success(request.authorizationRequest())
}