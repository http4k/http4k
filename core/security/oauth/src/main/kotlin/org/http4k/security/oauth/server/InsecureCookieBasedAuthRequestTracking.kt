package org.http4k.security.oauth.server

import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookie
import org.http4k.core.with

class InsecureCookieBasedAuthRequestTracking : AuthRequestTracking {
    private val cookieName = "OauthFlowId"

    override fun trackAuthRequest(request: Request, authRequest: AuthRequest, response: Response): Response =
        response.cookie(Cookie(cookieName, authRequest.serialise()))

    override fun resolveAuthRequest(request: Request): AuthRequest? =
        request.cookie(cookieName)?.value
            ?.let { Request(GET, Uri.of("dummy").query(it)) }?.authorizationRequest()

    private fun AuthRequest.serialise() = Request(GET, "dummy")
        .with(OAuthServer.clientIdQueryParameter of client)
        .with(OAuthServer.redirectUriQueryParameter of redirectUri!!)
        .with(OAuthServer.scopesQueryParameter of scopes)
        .with(OAuthServer.state of state)
        .with(OAuthServer.responseType of responseType)
        .with(OAuthServer.responseMode of responseMode)
        .uri.query
}
