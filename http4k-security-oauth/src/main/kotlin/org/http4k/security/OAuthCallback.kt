package org.http4k.security

import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.TEMPORARY_REDIRECT
import org.http4k.security.openid.IdToken
import org.http4k.security.openid.IdTokenConsumer

class OAuthCallback(
    private val oAuthPersistence: OAuthPersistence,
    private val idTokenConsumer: IdTokenConsumer,
    private val accessTokenFetcher: AccessTokenFetcher
) : HttpHandler {

    override fun invoke(request: Request) = request.queryOrFragmentParameter("code")
        ?.let { code ->
            val state = request.queryOrFragmentParameter("state")
            state?.let(::CrossSiteRequestForgeryToken)
                ?.takeIf { it == oAuthPersistence.retrieveCsrf(request) }
                ?.let {
                    val idToken = request.queryOrFragmentParameter("id_token")?.let { IdToken(it) }
                    if (hasValidNonceInIdToken(request, idToken)) {
                        idToken?.let { idTokenConsumer.consumeFromAuthorizationResponse(it) }
                        accessTokenFetcher.fetch(code)
                            ?.let { tokenDetails ->
                                tokenDetails.idToken?.also(idTokenConsumer::consumeFromAccessTokenResponse)
                                val originalUri = oAuthPersistence.retrieveOriginalUri(request)?.toString() ?: "/"
                                oAuthPersistence.assignToken(request, Response(TEMPORARY_REDIRECT)
                                    .header("Location", originalUri), tokenDetails.accessToken)
                            }
                    } else {
                        null
                    }
                }
        }
        ?: oAuthPersistence.authFailureResponse()

    private fun hasValidNonceInIdToken(request: Request, idToken: IdToken?): Boolean {
        return if (idToken != null) {
            idTokenConsumer.nonceFromIdToken(idToken) == oAuthPersistence.retrieveNonce(request)
        } else true
    }
}

private fun Request.queryOrFragmentParameter(name: String): String? = this.query(name) ?: fragmentParameter(name)
