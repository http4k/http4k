package org.http4k.security.oauth.server

import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.lens.Query
import org.http4k.lens.uri
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.security.AccessTokenContainer
import org.http4k.security.oauth.server.OAuthServer.Companion.clientId
import org.http4k.security.oauth.server.OAuthServer.Companion.redirectUri
import org.http4k.security.oauth.server.OAuthServer.Companion.scopes
import org.http4k.security.oauth.server.OAuthServer.Companion.state

class OAuthServer(
    tokenPath: String,
    validateClientAndRedirectionUri: ClientValidator,
    authorizationCodes: AuthorizationCodes,
    accessTokens: AccessTokens) {

    private val validationFilter = ClientAndRedirectionValidationFilter(validateClientAndRedirectionUri)

    val tokenRoute = routes(tokenPath bind POST to GenerateAccessToken(accessTokens))

    val authenticationStart = validationFilter

    val authenticationComplete = AuthenticationCompleteFilter(authorizationCodes, validationFilter)

    companion object {
        val clientId = Query.map(::ClientId, ClientId::value).required("client_id")
        val scopes = Query.map({ it.split(",").toList() }, { it.joinToString(",") }).optional("scopes")
        val redirectUri = Query.uri().required("redirect_uri")
        val state = Query.optional("state")
    }
}

interface AccessTokens {
    fun create(): AccessTokenContainer
}

interface AuthorizationCodes {
    fun create(): AuthorizationCode
}

typealias ClientValidator = (ClientId, Uri) -> Boolean

data class AuthorizationRequest(
        val client: ClientId,
        val scopes: List<String>,
        val redirectUri: Uri,
        val state: String?
)

internal fun Request.authorizationRequest() =
        AuthorizationRequest(
                clientId(this),
                scopes(this) ?: listOf(),
                redirectUri(this),
                state(this)
        )

data class ClientId(val value: String)
data class AuthorizationCode(val value: String)