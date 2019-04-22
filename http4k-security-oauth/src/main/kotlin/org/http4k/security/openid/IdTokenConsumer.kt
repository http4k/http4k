package org.http4k.security.openid

interface IdTokenConsumer {
    fun consumeFromAuthorizationResponse(idToken: IdTokenContainer)
    fun consumeFromAccessTokenResponse(idToken: IdTokenContainer)

    companion object {
        val NoOp = object : IdTokenConsumer {
            override fun consumeFromAccessTokenResponse(idToken: IdTokenContainer) = Unit
            override fun consumeFromAuthorizationResponse(idToken: IdTokenContainer) = Unit
        }
    }
}
