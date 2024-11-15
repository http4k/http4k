package org.http4k.connect.openai.auth.oauth

import org.http4k.security.oauth.server.AuthorizationCode

/**
 * Provides storage of the Principal against the OAuth authorisation code so it can be resolved later.
 */
interface PrincipalStore<Principal : Any> {
    operator fun get(key: AuthorizationCode): Principal?
    operator fun set(key: AuthorizationCode, data: Principal)
}
