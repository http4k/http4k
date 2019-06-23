package org.http4k.security.openid

interface IdTokenConsumer {
    fun consumeFromAuthorizationResponse(idToken: IdToken)
    fun consumeFromAccessTokenResponse(idToken: IdToken)

    companion object {
        val NoOp = object : IdTokenConsumer {
            override fun consumeFromAccessTokenResponse(idToken: IdToken) = Unit
            override fun consumeFromAuthorizationResponse(idToken: IdToken) = Unit
        }
    }
}
