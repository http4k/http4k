package org.http4k.security.oauth.server

import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.lens.Query
import org.http4k.lens.uri
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.security.oauth.server.OAuthServer.Companion.clientId
import org.http4k.security.oauth.server.OAuthServer.Companion.redirectUri
import org.http4k.security.oauth.server.OAuthServer.Companion.scopes
import org.http4k.security.oauth.server.OAuthServer.Companion.state
import java.time.Clock

/**
 * Provide help creating OAuth Authorization Server with Authorization Code Flow
 *
 * References:
 *  - Authorization Code Grant flow spec: https://tools.ietf.org/html/rfc6749#page-23
 *  - OAuth 2 Security Best Current Practices: https://tools.ietf.org/html/draft-ietf-oauth-security-topics-11
 */
class OAuthServer(
    tokenPath: String,
    authRequestPersistence: AuthRequestPersistence,
    clientValidator: ClientValidator,
    authorizationCodes: AuthorizationCodes,
    accessTokens: AccessTokens,
    clock: Clock
) {
    // endpoint to retrieve access token for a given authorization code
    val tokenRoute = routes(tokenPath bind POST to GenerateAccessToken(clientValidator, authorizationCodes, accessTokens, clock))

    // use this filter to protect your authentication/authorization pages
    val authenticationStart = ClientValidationFilter(clientValidator)
        .then(AuthRequestPersistenceFilter(authRequestPersistence))

    // use this filter to handle authorization code generation and redirection back to client
    val authenticationComplete = AuthenticationCompleteFilter(authorizationCodes, authRequestPersistence)

    companion object {
        val clientId = Query.map(::ClientId, ClientId::value).required("client_id")
        val scopes = Query.map({ it.split(" ").toList() }, { it.joinToString(" ") }).optional("scope")
        val redirectUri = Query.uri().required("redirect_uri")
        val state = Query.optional("state")
    }
}

data class AuthRequest(
        val client: ClientId,
        val scopes: List<String>,
        val redirectUri: Uri,
        val state: String?
)

internal fun Request.authorizationRequest() =
    AuthRequest(
                clientId(this),
                scopes(this) ?: listOf(),
                redirectUri(this),
                state(this)
        )

data class ClientId(val value: String)
data class AuthorizationCode(val value: String)