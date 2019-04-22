package org.http4k.security.oauth.server

import org.http4k.core.*
import org.http4k.core.Status.Companion.SEE_OTHER
import org.http4k.security.ResponseType.Code
import org.http4k.security.ResponseType.CodeIdToken

class AuthenticationCompleteFilter(
    private val authorizationCodes: AuthorizationCodes,
    private val requestTracking: AuthRequestTracking,
    private val idTokens: IdTokens
) : Filter {

    override fun invoke(next: HttpHandler): HttpHandler =
         { request ->
            val response = next(request)
            if (response.status.successful) {
                val authorizationRequest = requestTracking.resolveAuthRequest(request)
                    ?: error("Authorization request could not be found.")

                Response(SEE_OTHER)
                    .header("location", authorizationRequest.redirectUri
                        .addResponseTypeValues(authorizationRequest, request, response)
                        .query("state", authorizationRequest.state)
                        .toString())
            } else response
        }

    private fun Uri.addResponseTypeValues(authorizationRequest: AuthRequest, request: Request, response: Response) =
        with(authorizationCodes.create(request, authorizationRequest, response)) {
            when (authorizationRequest.responseType) {
                Code -> query("code", value)
                CodeIdToken -> query("code", value)
                    .query("id_token", idTokens.createForAuthorization(request, authorizationRequest, response).value)
            }
        }
}