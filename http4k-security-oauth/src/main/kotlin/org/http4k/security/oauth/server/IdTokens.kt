package org.http4k.security.oauth.server

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.security.openid.IdTokenContainer

interface IdTokens {

    fun createForAuthorization(request: Request, authRequest: AuthRequest, response: Response): IdTokenContainer
    fun createForAccessToken(code: AuthorizationCode): IdTokenContainer

    companion object {
        val Unsupported = object : IdTokens {
            override fun createForAccessToken(code: AuthorizationCode): IdTokenContainer {
                throw UnsupportedOperationException("ID Tokens are not supported by this server")
            }

            override fun createForAuthorization(request: Request, authRequest: AuthRequest, response: Response): IdTokenContainer {
                throw UnsupportedOperationException("ID Tokens are not supported by this server")
            }
        }
    }
}