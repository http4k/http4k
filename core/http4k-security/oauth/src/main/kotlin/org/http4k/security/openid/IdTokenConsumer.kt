package org.http4k.security.openid

import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.http4k.security.Nonce
import org.http4k.security.OAuthCallbackError

interface IdTokenConsumer {
    fun nonceFromIdToken(idToken: IdToken): Nonce?
    fun consumeFromAuthorizationResponse(idToken: IdToken): Result<Unit, OAuthCallbackError.InvalidIdToken>
    fun consumeFromAccessTokenResponse(idToken: IdToken): Result<Unit, OAuthCallbackError.InvalidIdToken>

    companion object {
        val NoOp = object : IdTokenConsumer {
            override fun nonceFromIdToken(idToken: IdToken): Nonce? = null
            override fun consumeFromAccessTokenResponse(idToken: IdToken) = Success(Unit)
            override fun consumeFromAuthorizationResponse(idToken: IdToken) = Success(Unit)
        }
    }
}
