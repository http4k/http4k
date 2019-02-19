package org.http4k.security.oauth.server

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.query

class AuthenticationCompleteFilter(
    private val authorizationCodes: AuthorizationCodes,
    private val persistence: OAuthRequestPersistence
) : Filter {

    override fun invoke(next: HttpHandler): HttpHandler {
        return { request ->
            val response = next(request)
            val authorizationRequest = persistence.retrieve(request)
            val postAuthResponse = if (response.status.successful) {
                Response(Status.TEMPORARY_REDIRECT)
                    .header("location", authorizationRequest.redirectUri
                        .query("code", authorizationCodes.create().value)
                        .query("state", authorizationRequest.state)
                        .toString())
            } else response
            persistence.clear(authorizationRequest, postAuthResponse)
        }
    }

}