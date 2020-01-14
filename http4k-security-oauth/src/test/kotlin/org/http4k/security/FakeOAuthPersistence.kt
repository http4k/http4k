package org.http4k.security

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.security.openid.Nonce

class FakeOAuthPersistence : OAuthPersistence {

    var csrf: CrossSiteRequestForgeryToken? = null
    var nonce: Nonce? = null
    var accessToken: AccessToken? = null

    override fun retrieveCsrf(request: Request): CrossSiteRequestForgeryToken? = csrf

    override fun assignCsrf(redirect: Response, csrf: CrossSiteRequestForgeryToken): Response {
        this.csrf = csrf
        return redirect.header("action", "assignCsrf")
    }

    override fun assignNonce(redirect: Response, nonce: Nonce): Response {
        this.nonce = nonce
        return redirect.header("action", "assignNonce")
    }

    override fun retrieveNonce(request: Request): Nonce? = nonce

    override fun retrieveToken(request: Request): AccessToken? = accessToken

    override fun assignToken(request: Request, redirect: Response, accessToken: AccessToken): Response {
        this.accessToken = accessToken
        return redirect.header("action", "assignToken")
    }
}
