package org.http4k.security.oauth.server

import org.http4k.core.Request
import org.http4k.core.Response

/**
 * Provides a mechanism to store OAuth authorization parameters to be used later
 * (i.e. can be used later to generate code and/or tokens)
 */
interface AuthRequestPersistence {
    /**
     * Assign a reference of AuthRequest to the response
     */
    fun storeAuthRequest(authRequest: AuthRequest, response: Response): Response

    /**
     * Retrieves a particular AuthRequest related to the particular request
     */
    fun retrieveAuthRequest(request: Request): AuthRequest?
}