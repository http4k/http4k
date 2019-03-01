package org.http4k.security

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.FORBIDDEN

/**
 * Provides persistence for OAuth lifecycle values:
 *  - CrossSiteRequestForgeryToken - used to retrieve that authorisation code assignment responses are genuinely from the end-service.
 *  - AccessTokenContainer - provides time-limited access to protected API resources on the end-service.
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
     * Assign the swapped AccessTokenContainer returned by the end-service. Opportunity here to modify the
     * response returned to the user when the redirection happens.
     */
    fun assignToken(request: Request, redirect: Response, accessToken: AccessTokenContainer): Response

    /**
     * Retrieve the stored AccessTokenContainer token for this user request
     */
    fun retrieveToken(request: Request): AccessTokenContainer?

    /**
     * Build the default failure response which occurs when a failure occurs during the callback process (eg. a mismatch/missing
     * CSRF or failure occurring when calling into the end-service for the access-token.
     */
    fun authFailureResponse() = Response(FORBIDDEN)
}