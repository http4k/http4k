package org.http4k.security.openid

import org.http4k.security.Nonce

interface IdTokenConsumer {
    fun nonceFromIdToken(idToken: IdToken): Nonce?
    fun consumeFromAuthorizationResponse(idToken: IdToken)
    fun consumeFromAccessTokenResponse(idToken: IdToken)

    companion object {
        val NoOp = object : IdTokenConsumer {
            override fun nonceFromIdToken(idToken: IdToken): Nonce? = null
            override fun consumeFromAccessTokenResponse(idToken: IdToken) = Unit
            override fun consumeFromAuthorizationResponse(idToken: IdToken) = Unit
        }
    }
}
