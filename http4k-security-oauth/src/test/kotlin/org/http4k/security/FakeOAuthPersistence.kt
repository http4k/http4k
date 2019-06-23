package org.http4k.security

import org.http4k.core.Request
import org.http4k.core.Response

class FakeOAuthPersistence : OAuthPersistence {

    var csrf: CrossSiteRequestForgeryToken? = null
    var accessToken: AccessToken? = null

    override fun retrieveCsrf(request: Request): CrossSiteRequestForgeryToken? = csrf

    override fun assignCsrf(redirect: Response, csrf: CrossSiteRequestForgeryToken): Response {
        this.csrf = csrf
        return redirect.header("action", "assignCsrf")
    }

    override fun retrieveToken(request: Request): AccessToken? = accessToken

    override fun assignToken(request: Request, redirect: Response, accessToken: AccessToken): Response {
        this.accessToken = accessToken
        return redirect.header("action", "assignToken")
    }
}