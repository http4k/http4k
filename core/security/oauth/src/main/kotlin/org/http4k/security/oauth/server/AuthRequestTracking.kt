package org.http4k.security.oauth.server

import org.http4k.core.Request
import org.http4k.core.Response

/**
 * Provides a mechanism to track OAuth authorization parameters to be used later
 * (i.e. can be used later to generate code and/or tokens)
 */
interface AuthRequestTracking {
    /**
     * Assign a reference of AuthRequest to the response
     */
    fun trackAuthRequest(request: Request, authRequest: AuthRequest, response: Response): Response

    /**
     * Resolves a particular AuthRequest related to the particular request
     */
    fun resolveAuthRequest(request: Request): AuthRequest?
}
