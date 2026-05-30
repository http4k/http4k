package org.http4k.security.oauth.server

import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.mapFailure
import org.http4k.core.Request
import org.http4k.core.Response

fun interface AuthorisationGuard {
    fun guard(request: Request, authRequest: AuthRequest): Result<Request, Response>

    companion object {
        val AlwaysValid: AuthorisationGuard = AuthorisationGuard { request, _ -> Success(request) }

        operator fun invoke(
            validator: AuthoriseRequestValidator,
            errorRender: AuthoriseRequestErrorRender
        ): AuthorisationGuard = AuthorisationGuard { request, authRequest ->
            validator.validate(request, authRequest).mapFailure { errorRender.errorFor(request, it) }
        }
    }
}
