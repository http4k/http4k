package org.http4k.security.openid

import org.http4k.security.State
import org.http4k.security.oauth.server.AuthRequest

interface RequestJwts {
    fun create(authRequest: AuthRequest, state: State, nonce: Nonce?): RequestJwtContainer
}

data class RequestJwtContainer(val value: String)
