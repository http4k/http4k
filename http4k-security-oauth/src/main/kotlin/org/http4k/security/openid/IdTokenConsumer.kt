package org.http4k.security.openid

interface IdTokenConsumer {
    fun consume(idToken: IdTokenContainer)

    companion object {
        val NoOp = object : IdTokenConsumer {
            override fun consume(idToken: IdTokenContainer) = Unit
        }
    }
}
