package org.http4k.security.oauth.server

import org.http4k.core.Filter
import org.http4k.core.HttpHandler

class AuthRequestTrackingFilter(private val tracking: AuthRequestTracking) : Filter {
    override fun invoke(next: HttpHandler): HttpHandler {
        return { request ->
            val authorizationRequest = request.authorizationRequest()
            val response = next(request)
            tracking.trackAuthRequest(authorizationRequest, response)
        }
    }
}