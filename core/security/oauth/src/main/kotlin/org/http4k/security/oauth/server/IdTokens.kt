package org.http4k.security.oauth.server

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.security.AccessToken
import org.http4k.security.Nonce
import org.http4k.security.openid.IdToken

interface IdTokens {

    fun createForAuthorization(
        request: Request, authRequest: AuthRequest, response: Response,
        nonce: Nonce?, code: AuthorizationCode
    ): IdToken

    fun createForAccessToken(
        authorizationCodeDetails: AuthorizationCodeDetails,
        code: AuthorizationCode,
        accessToken: AccessToken
    ): IdToken

    companion object {
        val Unsupported = object : IdTokens {
            override fun createForAuthorization(
                request: Request, authRequest: AuthRequest, response: Response,
                nonce: Nonce?, code: AuthorizationCode
            ): IdToken {
                throw UnsupportedOperationException("ID Tokens are not supported by this server")
            }

            override fun createForAccessToken(
                authorizationCodeDetails: AuthorizationCodeDetails,
                code: AuthorizationCode,
                accessToken: AccessToken
            ): IdToken {
                throw UnsupportedOperationException("ID Tokens are not supported by this server")
            }
        }
    }
}
