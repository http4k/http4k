package org.http4k.security.oauth.server

import org.http4k.core.Uri

/**
 * Provides a consistent way to validate clients attempting to use an authorization code flow
 */
interface ClientValidator {
    /**
     * Client validation must include:
     * - check that client_id is a valid, registered app
     * - redirection URI is one of the allowed ones for that app
     */
    fun validateRedirection(clientId: ClientId, redirectionUri: Uri): Boolean

    /**
     * Validate that credentials provided by the client match its registration records
     */
    fun validateCredentials(clientId: ClientId, clientSecret: String): Boolean
}