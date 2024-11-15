package org.http4k.connect.amazon.cognito.oauth

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.lens.LensFailure
import org.http4k.security.oauth.server.AuthRequest
import org.http4k.security.oauth.server.AuthRequestTracking
import org.http4k.security.oauth.server.OAuthServer

class InMemoryAuthRequestTracking : AuthRequestTracking {
    private val inFlightRequests = mutableListOf<AuthRequest>()

    override fun trackAuthRequest(request: Request, authRequest: AuthRequest, response: Response) =
        response.also { inFlightRequests += authRequest }

    override fun resolveAuthRequest(request: Request) =
        try {
            with(OAuthServer) {
                val extracted = AuthRequest(
                    clientIdQueryParameter(request),
                    scopesQueryParameter(request) ?: listOf(),
                    redirectUriQueryParameter(request),
                    state(request),
                    responseType(request)
                )
                if (inFlightRequests.remove(extracted)) extracted else null
            }
        } catch (e: LensFailure) {
            null
        }
}
