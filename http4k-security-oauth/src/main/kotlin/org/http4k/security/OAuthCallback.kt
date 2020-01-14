package org.http4k.security

import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.TEMPORARY_REDIRECT
import org.http4k.core.toParameters
import org.http4k.security.openid.IdToken
import org.http4k.security.openid.IdTokenConsumer

class OAuthCallback(
    private val oAuthPersistence: OAuthPersistence,
    private val idTokenConsumer: IdTokenConsumer,
    private val accessTokenFetcher: AccessTokenFetcher
) : HttpHandler {

    override fun invoke(request: Request) = request.queryOrFragmentParameter("code")
        ?.let { code ->
            val state = request.queryOrFragmentParameter("state")?.toParameters() ?: emptyList()
            state.find { it.first == "csrf" }?.second
                ?.let(::CrossSiteRequestForgeryToken)
                ?.takeIf { it == oAuthPersistence.retrieveCsrf(request) }
                ?.let {
                    request.queryOrFragmentParameter("id_token")?.let { idTokenConsumer.consumeFromAuthorizationResponse(IdToken(it)) }
                    accessTokenFetcher.fetch(code)
                        ?.let { tokenDetails ->
                            tokenDetails.idToken?.also(idTokenConsumer::consumeFromAccessTokenResponse)

                            val originalUri = state.find { it.first == "uri" }?.second ?: "/"
                            oAuthPersistence.assignToken(request, Response(TEMPORARY_REDIRECT)
                                .header("Location", originalUri), tokenDetails.accessToken)
                        }
                }
        }
        ?: oAuthPersistence.authFailureResponse()
}

private fun Request.queryOrFragmentParameter(name: String): String? = this.query(name) ?: fragmentParameter(name)
