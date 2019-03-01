package org.http4k.security.oauth.server

import org.http4k.core.Filter
import org.http4k.core.HttpHandler

class AuthRequestPersistenceFilter(
    private val persistence: AuthRequestPersistence
) : Filter {
    override fun invoke(next: HttpHandler): HttpHandler {
        return { request ->
            val authorizationRequest = request.authorizationRequest()
            val response = next(request)
            persistence.storeAuthRequest(authorizationRequest, response)
        }
    }
}