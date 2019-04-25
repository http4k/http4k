package org.http4k.security.oauth.server

import org.http4k.security.AccessTokenContainer

data class AccessTokenResult(val token : AccessTokenContainer? = null, val error: AccessTokenCreationError? = null) {
    fun isSuccess() : Boolean {
        return token != null
    }
}

enum class AccessTokenCreationError {
    AUTHORIZATION_CODE_ALREADY_USED
}
