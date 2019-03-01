package org.http4k.security.oauth.server

import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookie
import org.http4k.core.with

class InsecureCookieBasedAuthRequestPersistence : AuthRequestPersistence {
    private val cookieName = "OauthFlowId"

    override fun storeAuthRequest(authRequest: AuthRequest, response: Response): Response =
        response.cookie(Cookie(cookieName, authRequest.serialise()))

    override fun retrieveAuthRequest(request: Request): AuthRequest? =
        request.cookie(cookieName)?.value
            ?.let { Request(Method.GET, Uri.of("dummy").query(it)) }
            ?.let { it.authorizationRequest() }

    private fun AuthRequest.serialise() = Request(Method.GET, "dummy")
        .with(OAuthServer.clientId of client)
        .with(OAuthServer.redirectUri of redirectUri)
        .with(OAuthServer.scopes of scopes)
        .with(OAuthServer.state of state)
        .uri.query
}