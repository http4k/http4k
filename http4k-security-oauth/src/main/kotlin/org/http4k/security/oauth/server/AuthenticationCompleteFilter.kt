package org.http4k.security.oauth.server

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.query
import org.http4k.core.then

class AuthenticationCompleteFilter(
    private val authorizationCodes: AuthorizationCodes,
    private val validationFilter: ClientAndRedirectionValidationFilter
) : Filter {

    override fun invoke(next: HttpHandler): HttpHandler =
        validationFilter.then { request ->
            val response = next(request)
            if (response.status.successful) {
                val authorizationRequest = request.authorizationRequest()
                Response(Status.TEMPORARY_REDIRECT)
                    .header("location", authorizationRequest.redirectUri
                        .query("code", authorizationCodes.create().value)
                        .query("state", authorizationRequest.state)
                        .toString())
            } else response
        }
}