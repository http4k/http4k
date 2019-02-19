package org.http4k.security.oauth.server

/**
 * Provides a consistent way to manage authorization codes
 */
interface AuthorizationCodes {
    /**
     * Create new authorization code to be given to client after the user successfully authorize access
     */
    fun create(): AuthorizationCode
}