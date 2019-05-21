package org.http4k.security.oauth.server

import org.http4k.core.*
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookie

class InsecureCookieBasedAuthRequestTracking : AuthRequestTracking {
    private val cookieName = "OauthFlowId"

    override fun trackAuthRequest(request: Request, authRequest: AuthRequest, response: Response): Response =
        response.cookie(Cookie(cookieName, authRequest.serialise()))

    override fun resolveAuthRequest(request: Request): AuthRequest? =
        request.cookie(cookieName)?.value
            ?.let { Request(Method.GET, Uri.of("dummy").query(it)) }
            ?.let { it.authorizationRequest() }

    private fun AuthRequest.serialise() = Request(Method.GET, "dummy")
        .with(OAuthServer.clientId of client)
        .with(OAuthServer.redirectUri of redirectUri)
        .with(OAuthServer.scopes of scopes)
        .with(OAuthServer.state of state)
        .with(OAuthServer.responseType of responseType)
        .uri.query
}