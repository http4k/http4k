package org.http4k.security.oauth.server

import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookie
import org.http4k.core.cookie.invalidateCookie
import org.http4k.core.with
import org.http4k.lens.Query
import org.http4k.lens.uuid

class InsecureCookieBasedOAuthRequestPersistence : OAuthRequestPersistence {
    private val cookieName = "OauthRequest"

    override fun store(authorizationRequest: AuthorizationRequest, response: Response): Response {
        val parameters = DUMMY
                .with(id of authorizationRequest.id)
                .with(OAuthServer.clientId of authorizationRequest.client)
                .with(OAuthServer.scopes of authorizationRequest.scopes)
                .with(OAuthServer.redirectUri of authorizationRequest.redirectUri)
                .with(OAuthServer.state of authorizationRequest.state)
        return response.cookie(Cookie(cookieName, parameters.uri.query))
    }

    override fun retrieve(request: Request): AuthorizationRequest {
        val parameters = DUMMY.uri(DUMMY.uri.query(request.cookie(cookieName)?.value.orEmpty()))
        return parameters.authorizationRequest(id(parameters))
    }

    override fun clear(authorizationRequest: AuthorizationRequest, response: Response): Response =
            response.invalidateCookie(cookieName)

    companion object {
        private val DUMMY = Request(Method.GET, Uri.of("/dummy"))
        val id = Query.uuid().required("id")
    }
}