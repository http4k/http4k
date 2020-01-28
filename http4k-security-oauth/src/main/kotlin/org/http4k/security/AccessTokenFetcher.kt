package org.http4k.security

import org.http4k.core.ContentType.Companion.APPLICATION_FORM_URLENCODED
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.body.form
import org.http4k.core.with
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.security.openid.IdToken

class AccessTokenFetcher(
    private val api: HttpHandler,
    private val callbackUri: Uri,
    private val providerConfig: OAuthProviderConfig,
    private val accessTokenFetcherAuthenticator: AccessTokenFetcherAuthenticator
) {
    fun fetch(code: String): AccessTokenDetails? = api(Request(POST, providerConfig.tokenPath)
        .with(CONTENT_TYPE of APPLICATION_FORM_URLENCODED)
        .form("grant_type", "authorization_code")
        .form("redirect_uri", callbackUri.toString())
        .form("client_id", providerConfig.credentials.user)
        .authenticate(accessTokenFetcherAuthenticator)
        .form("code", code))
        .let { if (it.status != OK) null else it }
        ?.let { msg ->
            CONTENT_TYPE(msg)?.takeIf { APPLICATION_JSON.equalsIgnoringDirectives(it) }
                ?.let {
                    with(accessTokenResponseBody(msg)) {
                        AccessTokenDetails(AccessToken(accessToken), idToken?.let(::IdToken))
                    }
                } ?: AccessTokenDetails(AccessToken(msg.bodyString()))
        }

    private fun Request.authenticate(accessTokenFetcherAuthenticator: AccessTokenFetcherAuthenticator): Request =
        accessTokenFetcherAuthenticator.authenticate(this)
}
