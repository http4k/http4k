package org.http4k.security

import org.http4k.core.ContentType
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.core.Uri
import org.http4k.core.body.form
import org.http4k.core.with
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.security.openid.IdTokenContainer

class AccessTokenFetcher(
    private val api: HttpHandler,
    private val callbackUri: Uri,
    private val providerConfig: OAuthProviderConfig
) {
    fun fetch(code: String): AccessTokenDetails? = api(Request(POST, providerConfig.tokenPath)
        .with(CONTENT_TYPE of ContentType.APPLICATION_FORM_URLENCODED)
        .form("grant_type", "authorization_code")
        .form("redirect_uri", callbackUri.toString())
        .form("client_id", providerConfig.credentials.user)
        .form("client_secret", providerConfig.credentials.password)
        .form("code", code))
        .let { if (it.status != Status.OK) null else it }
        ?.let {
            if (CONTENT_TYPE(it) == ContentType.APPLICATION_JSON) {
                with(accessTokenResponseBody(it)) {
                    AccessTokenDetails(AccessTokenContainer(accessToken), idToken?.let(::IdTokenContainer))
                }
            } else
                AccessTokenDetails(AccessTokenContainer(it.bodyString()))
        }
}
