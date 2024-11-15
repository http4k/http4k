package org.http4k.security

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.security.openid.IdToken

class FakeOAuthPersistence : OAuthPersistence {
    private var csrf: CrossSiteRequestForgeryToken? = null
    private var nonce: Nonce? = null
    private var accessToken: AccessToken? = null
    private var originalUri: Uri? = null
    private var pkce: PkceChallengeAndVerifier? = null

    override fun assignNonce(redirect: Response, nonce: Nonce): Response {
        this.nonce = nonce
        return redirect.header("action", "assignNonce")
    }

    override fun assignToken(request: Request, redirect: Response, accessToken: AccessToken, idToken: IdToken?): Response {
        this.accessToken = accessToken
        return redirect.header("action", "assignToken")
    }

    override fun assignCsrf(redirect: Response, csrf: CrossSiteRequestForgeryToken): Response {
        this.csrf = csrf
        return redirect.header("action", "assignCsrf")
    }

    override fun assignOriginalUri(redirect: Response, originalUri: Uri): Response {
        this.originalUri = originalUri
        return redirect.header("action", "assignOriginalUri")
    }

    override fun assignPkce(redirect: Response, pkce: PkceChallengeAndVerifier): Response {
        this.pkce = pkce
        return redirect.header("action", "assignPkce")
    }

    override fun retrieveNonce(request: Request): Nonce? = nonce

    override fun retrieveToken(request: Request): AccessToken? = accessToken

    override fun retrieveCsrf(request: Request): CrossSiteRequestForgeryToken? = csrf

    override fun retrieveOriginalUri(request: Request): Uri? = originalUri

    override fun retrievePkce(request: Request): PkceChallengeAndVerifier? = pkce
}
