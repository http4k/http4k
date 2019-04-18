package org.http4k.security.oauth.server

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status.Companion.SEE_OTHER
import org.http4k.core.query

class AuthenticationCompleteFilter(
    private val authorizationCodes: AuthorizationCodes,
    private val requestTracking: AuthRequestTracking
) : Filter {

    override fun invoke(next: HttpHandler): HttpHandler =
         { request ->
            val response = next(request)
            if (response.status.successful) {
                val authorizationRequest = requestTracking.resolveAuthRequest(request)
                    ?: error("Authorization request could not be found.")

                val code = authorizationCodes.create(request, authorizationRequest, response)

                Response(SEE_OTHER)
                    .header("location", authorizationRequest.redirectUri
                        .query("code", code.value)
                        .query("state", authorizationRequest.state)
                        .toString())
            } else response
        }
}