package org.http4k.security

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.core.Uri
import org.http4k.security.openid.Nonce

/**
 * Provides persistence for OAuth lifecycle values:
 *  - CrossSiteRequestForgeryToken - used to retrieve that authorisation code assignment responses are genuinely from the end-service.
 *  - AccessToken - provides time-limited access to protected API resources on the end-service.
 */
interface OAuthPersistence {

    /**
     * Assign a CSRF token to this OAuth auth redirection (to the end-service) response. Opportunity here to modify the
     * response returned to the user when the redirection happens.
     */
    fun assignCsrf(redirect: Response, csrf: CrossSiteRequestForgeryToken): Response

    /**
     * Retrieve the stored CSRF token for this user request
     */
    fun retrieveCsrf(request: Request): CrossSiteRequestForgeryToken?

    /**
     * Assign a nonce to this OIDC auth redirection (to the end-service) response. Opportunity here to modify the
     * response returned to the user when the redirection happens.
     */
    fun assignNonce(redirect: Response, nonce: Nonce): Response

    /**
     * Retrieve the stored nonce token for this user request
     */
    fun retrieveNonce(request: Request): Nonce?

    /**
     * opportunity to store the uri that the request was made before authentication
     * this will then be redirected back to after auth
     *
     * Be careful not to create scenarios where an open redirector is created,
     * by applications attempting some sort of phishing attack
     */
    fun assignOriginalUri(redirect: Response, originalUri: Uri): Response

    /**
     * Retrieve the stored original uri for this user request
     */
    fun retrieveOriginalUri(request: Request): Uri?


    /**
     * Assign the swapped AccessToken returned by the end-service. Opportunity here to modify the
     * response returned to the user when the redirection happens.
     */
    fun assignToken(request: Request, redirect: Response, accessToken: AccessToken): Response

    /**
     * Retrieve the stored AccessToken token for this user request
     */
    fun retrieveToken(request: Request): AccessToken?

    /**
     * Build the default failure response which occurs when a failure occurs during the callback process (eg. a mismatch/missing
     * CSRF or failure occurring when calling into the end-service for the access-token.
     */
    fun authFailureResponse() = Response(FORBIDDEN)
}
