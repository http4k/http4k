package org.http4k.security.oauth.server

import org.http4k.core.Request
import org.http4k.core.Response

interface AuthRequestPersistence {
    fun storeAuthRequest(authRequest: AuthRequest, response: Response): Response
    fun retrieveAuthRequest(request: Request): AuthRequest?
}