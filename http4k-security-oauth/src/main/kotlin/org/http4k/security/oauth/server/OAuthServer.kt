package org.http4k.security.oauth.server

import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.then
import org.http4k.filter.ServerFilters
import org.http4k.format.AutoMarshallingJson
import org.http4k.lens.Query
import org.http4k.lens.uri
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.security.AccessTokenContainer
import org.http4k.security.ResponseType
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
    authRequestTracking: AuthRequestTracking,
    clientValidator: ClientValidator,
    authorizationCodes: AuthorizationCodes,
    accessTokens: AccessTokens,
    json: AutoMarshallingJson,
    clock: Clock,
    authRequestExtractor: AuthRequestExtractor = BasicAuthRequestExtractor,
    idTokens: IdTokens = IdTokens.Unsupported,
    documentationUri: String? = null
) {
    private val errorRenderer = ErrorRenderer(json, documentationUri)
    // endpoint to retrieve access token for a given authorization code
    val tokenRoute = routes(tokenPath bind POST to GenerateAccessToken(clientValidator, authorizationCodes, accessTokens, clock, idTokens, errorRenderer))

    // use this filter to protect your authentication/authorization pages
    val authenticationStart = ClientValidationFilter(clientValidator, errorRenderer, authRequestExtractor)
        .then(AuthRequestTrackingFilter(authRequestTracking, BasicAuthRequestExtractor, errorRenderer))

    // endpoint to handle authorization code generation and redirection back to client
    val authenticationComplete = AuthenticationComplete(authorizationCodes, authRequestTracking, idTokens, documentationUri)

    //use this filter to protect endpoints which should only be accessed with a valid access token
    val accessFilter = AccessTokenValidationFilter(accessTokens)

    companion object {
        val clientId = Query.map(::ClientId, ClientId::value).required("client_id")
        val scopes = Query.map({ it.split(" ").toList() }, { it.joinToString(" ") }).optional("scope")
        val redirectUri = Query.uri().required("redirect_uri")
        val state = Query.optional("state")
        val responseType = Query.map(ResponseType.Companion::fromQueryParameterValue, ResponseType::queryParameterValue).required("response_type")
    }
}

data class ClientId(val value: String)

data class AuthorizationCode(val value: String)

internal fun Request.authorizationRequest() =
    AuthRequest(
        OAuthServer.clientId(this),
        OAuthServer.scopes(this) ?: listOf(),
        OAuthServer.redirectUri(this),
        OAuthServer.state(this),
        OAuthServer.responseType(this)
    )