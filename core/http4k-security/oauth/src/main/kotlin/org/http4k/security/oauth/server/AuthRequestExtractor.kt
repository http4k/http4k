package org.http4k.security.oauth.server

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.http4k.core.Request
import org.http4k.lens.LensFailure

fun interface AuthRequestExtractor {
    fun extract(request: Request): Result<AuthRequest, InvalidAuthorizationRequest>
}

object AuthRequestFromQueryParameters : AuthRequestExtractor {
    override fun extract(request: Request): Result<AuthRequest, InvalidAuthorizationRequest> =
        try {
            Success(request.authorizationRequest())
        } catch (e: LensFailure) {
            Failure(InvalidAuthorizationRequest(e.failures.joinToString("; ")))
        }
}
