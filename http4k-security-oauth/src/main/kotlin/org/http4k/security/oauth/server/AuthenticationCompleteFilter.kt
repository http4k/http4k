package org.http4k.security.oauth.server

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.query

class AuthenticationCompleteFilter(
    private val authorizationCodes: AuthorizationCodes,
    private val requestPersistence: AuthRequestPersistence
) : Filter {

    override fun invoke(next: HttpHandler): HttpHandler =
         { request ->
            val response = next(request)
            if (response.status.successful) {
                val authorizationRequest = requestPersistence.retrieveAuthRequest(request) ?: error("authorization request could not be found!")
                val code = authorizationCodes.create(
                        authorizationRequest.client,
                    authorizationRequest.redirectUri
                )
                Response(Status.TEMPORARY_REDIRECT)
                    .header("location", authorizationRequest.redirectUri
                        .query("code", code.value)
                        .query("state", authorizationRequest.state)
                        .toString())
            } else response
        }
}