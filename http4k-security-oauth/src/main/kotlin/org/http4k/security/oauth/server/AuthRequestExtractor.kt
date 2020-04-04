package org.http4k.security.oauth.server

import org.http4k.core.Request
import org.http4k.lens.LensFailure
import org.http4k.util.Failure
import org.http4k.util.Result
import org.http4k.util.Success

interface AuthRequestExtractor {
    fun extract(request: Request): Result<InvalidAuthorizationRequest, AuthRequest>
}

object AuthRequestFromQueryParameters : AuthRequestExtractor {
    override fun extract(request: Request): Result<InvalidAuthorizationRequest, AuthRequest> =
        try {
            Success(request.authorizationRequest())
        } catch (e: LensFailure) {
            Failure(InvalidAuthorizationRequest(e.failures.joinToString("; ")))
        }
}
