package org.http4k.security.oauth.server

import org.http4k.core.Body
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.then
import org.http4k.format.AutoMarshallingJson
import org.http4k.lens.FormField
import org.http4k.lens.Query
import org.http4k.lens.Validator.Strict
import org.http4k.lens.uri
import org.http4k.lens.webForm
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.security.ResponseMode
import org.http4k.security.ResponseType
import org.http4k.security.oauth.server.accesstoken.AccessTokenRequestAuthentication
import org.http4k.security.oauth.server.accesstoken.ClientSecretAccessTokenRequestAuthentication
import org.http4k.security.oauth.server.accesstoken.GrantType
import org.http4k.security.oauth.server.accesstoken.GrantTypesConfiguration
import org.http4k.security.openid.Nonce
import org.http4k.security.openid.RequestJwtContainer
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
    authoriseRequestValidator: AuthoriseRequestValidator,
    accessTokenRequestAuthentication: AccessTokenRequestAuthentication,
    authorizationCodes: AuthorizationCodes,
    accessTokens: AccessTokens,
    json: AutoMarshallingJson,
    clock: Clock,
    authRequestExtractor: AuthRequestExtractor = AuthRequestFromQueryParameters,
    grantTypes: GrantTypesConfiguration = GrantTypesConfiguration.default(accessTokenRequestAuthentication),
    idTokens: IdTokens = IdTokens.Unsupported,
    documentationUri: String? = null
) {

    constructor(tokenPath: String,
                authRequestTracking: AuthRequestTracking,
                clientValidator: ClientValidator,
                authorizationCodes: AuthorizationCodes,
                accessTokens: AccessTokens,
                json: AutoMarshallingJson,
                clock: Clock,
                authRequestExtractor: AuthRequestExtractor = AuthRequestFromQueryParameters,
                grantTypes: GrantTypesConfiguration = GrantTypesConfiguration.default(ClientSecretAccessTokenRequestAuthentication(clientValidator)),
                idTokens: IdTokens = IdTokens.Unsupported,
                documentationUri: String? = null) : this(
        tokenPath,
        authRequestTracking,
        SimpleAuthoriseRequestValidator(clientValidator),
        ClientSecretAccessTokenRequestAuthentication(clientValidator),
        authorizationCodes,
        accessTokens,
        json,
        clock,
        authRequestExtractor,
        grantTypes,
        idTokens,
        documentationUri
    )

    private val errorRenderer = JsonResponseErrorRenderer(json, documentationUri)
    private val authoriseRequestErrorRender = AuthoriseRequestErrorRender(authoriseRequestValidator, errorRenderer, documentationUri)
    // endpoint to retrieve access token for a given authorization code
    val tokenRoute = routes(tokenPath bind POST to GenerateAccessToken(authorizationCodes, accessTokens, clock, idTokens, errorRenderer, grantTypes))

    // use this filter to protect your authentication/authorization pages
    val authenticationStart = ClientValidationFilter(authoriseRequestValidator, authoriseRequestErrorRender, authRequestExtractor)
        .then(AuthRequestTrackingFilter(authRequestTracking, authRequestExtractor, authoriseRequestErrorRender))

    // endpoint to handle authorization code generation and redirection back to client
    val authenticationComplete = AuthenticationComplete(authorizationCodes, authRequestTracking, idTokens, documentationUri)

    companion object {
        val clientIdQueryParameter = Query.map(::ClientId, ClientId::value).required("client_id")
        val scopesQueryParameter = Query.map({ it.split(" ").toList() }, { it.joinToString(" ") }).optional("scope")
        val redirectUriQueryParameter = Query.uri().required("redirect_uri")
        val state = Query.optional("state")
        val responseType = Query.map(ResponseType.Companion::fromQueryParameterValue, ResponseType::queryParameterValue).required("response_type")
        val responseMode = Query.map(ResponseMode.Companion::fromQueryParameterValue, ResponseMode::queryParameterValue).optional("response_mode")
        val nonce = Query.map(::Nonce, Nonce::value).optional("nonce")
        val request = Query.map(::RequestJwtContainer, RequestJwtContainer::value).optional("request")

        val clientIdForm = FormField.map(::ClientId, ClientId::value).optional("client_id")
        val clientSecret = FormField.optional("client_secret")
        val code = FormField.optional("code")
        val redirectUriForm = FormField.uri().optional("redirect_uri")
        val scopesForm = FormField.map({ it.split(" ").toList() }, { it.joinToString(" ") }).optional("scope")
        val clientAssertionType = FormField.uri().optional("client_assertion_type")
        val clientAssertion = FormField.optional("client_assertion")
        val tokenRequestWebForm = Body.webForm(
            Strict,
            clientIdForm,
            clientSecret,
            code,
            redirectUriForm,
            scopesForm,
            clientAssertionType,
            clientAssertion).toLens()
    }
}

data class ClientId(val value: String)

data class AuthorizationCode(val value: String)

internal fun Request.authorizationRequest() =
    AuthRequest(
        OAuthServer.clientIdQueryParameter(this),
        OAuthServer.scopesQueryParameter(this) ?: listOf(),
        OAuthServer.redirectUriQueryParameter(this),
        OAuthServer.state(this),
        OAuthServer.responseType(this),
        OAuthServer.nonce(this),
        OAuthServer.responseMode(this),
        OAuthServer.request(this)
    )

internal fun Request.tokenRequest(grantType: GrantType): TokenRequest {
    val tokenRequestWebForm = OAuthServer.tokenRequestWebForm(this)
    return TokenRequest(
        grantType,
        OAuthServer.clientIdForm(tokenRequestWebForm),
        OAuthServer.clientSecret(tokenRequestWebForm),
        OAuthServer.code(tokenRequestWebForm),
        OAuthServer.redirectUriForm(tokenRequestWebForm),
        OAuthServer.scopesForm(tokenRequestWebForm) ?: listOf(),
        OAuthServer.clientAssertionType(tokenRequestWebForm),
        OAuthServer.clientAssertion(tokenRequestWebForm))
}
