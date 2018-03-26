package org.http4k.security

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.FORBIDDEN

/**
 * Provides persistence for OAuth lifecycle values:
 *  - CrossSiteRequestForgeryToken - used to validate that authorisation code assignment responses are genuinely from the end-service.
 *  - AccessToken - provides time-limited access to protected API resources on the end-service.
 */
interface OAuthPersistence {

    /**
     * Assign a CSRF token to this OAuth auth redirection (to the end-service) response
     */
    fun assignCsrf(redirect: Response, csrf: CrossSiteRequestForgeryToken): Response

    /**
     * Retrieve the stored CSRF token for this user request
     */
    fun retrieveCsrf(request: Request): CrossSiteRequestForgeryToken?

    /**
     * Assign the swapped AccessToken returned by the end-service
     */
    fun assignToken(request: Request, redirect: Response, accessToken: AccessToken): Response

    /**
     * Retrieve the stored AccessToken token for this user request
     */
    fun retrieveToken(request: Request): AccessToken?

    fun authFailureResponse() = Response(FORBIDDEN)
}