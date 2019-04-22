package org.http4k.security.oauth.server

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.security.openid.IdTokenContainer

interface IdTokens {

    fun createForAuthorization(request: Request, authRequest: AuthRequest, response: Response): IdTokenContainer

    companion object {
        val Unsupported = object : IdTokens {
            override fun createForAuthorization(request: Request, authRequest: AuthRequest, response: Response): IdTokenContainer {
                throw UnsupportedOperationException("ID Tokens are not supported by this server")
            }
        }
    }
}