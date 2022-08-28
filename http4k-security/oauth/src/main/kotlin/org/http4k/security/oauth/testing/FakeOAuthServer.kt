package org.http4k.security.oauth.testing

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.FOUND
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.lens.Header.LOCATION
import org.http4k.lens.LensFailure
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.security.AccessToken
import org.http4k.security.oauth.server.AccessTokenResponseRenderer
import org.http4k.security.oauth.server.AccessTokens
import org.http4k.security.oauth.server.AuthRequest
import org.http4k.security.oauth.server.AuthRequestTracking
import org.http4k.security.oauth.server.AuthorizationCode
import org.http4k.security.oauth.server.AuthorizationCodeDetails
import org.http4k.security.oauth.server.AuthorizationCodes
import org.http4k.security.oauth.server.ClientId
import org.http4k.security.oauth.server.ClientValidator
import org.http4k.security.oauth.server.DefaultAccessTokenResponseRenderer
import org.http4k.security.oauth.server.OAuthServer
import org.http4k.security.oauth.server.TokenRequest
import org.http4k.security.oauth.server.UnsupportedGrantType
import org.http4k.security.oauth.server.accesstoken.AuthorizationCodeAccessTokenRequest
import java.time.Clock
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * This Server provides auto-login functionality without the need for user action.
 */
fun FakeOAuthServer(
    authPath: String,
    tokenPath: String,
    clock: Clock = Clock.systemDefaultZone(),
    accessTokens: AccessTokens = SimpleAccessTokens(),
    tokenResponseRenderer: AccessTokenResponseRenderer = DefaultAccessTokenResponseRenderer
): RoutingHttpHandler {
    val server = OAuthServer(
        tokenPath,
        InMemoryAuthRequestTracking(),
        AlwaysOkClientValidator(),
        InMemoryAuthorizationCodes(clock),
        accessTokens,
        clock,
        tokenResponseRenderer = tokenResponseRenderer
    )

    return routes(
        server.tokenRoute,
        authPath bind GET to server.authenticationStart.then {
            Response(FOUND).with(LOCATION of it.uri.path("/autologin"))
        },
        "/autologin" bind GET to { server.authenticationComplete(it) }
    )
}

private class AlwaysOkClientValidator : ClientValidator {
    override fun validateClientId(request: Request, clientId: ClientId) = true

    override fun validateCredentials(request: Request, clientId: ClientId, clientSecret: String) = true

    override fun validateRedirection(request: Request, clientId: ClientId, redirectionUri: Uri) = true

    override fun validateScopes(request: Request, clientId: ClientId, scopes: List<String>) = true
}

private class InMemoryAuthorizationCodes(private val clock: Clock) : AuthorizationCodes {
    private val inFlightCodes = mutableMapOf<AuthorizationCode, AuthorizationCodeDetails>()

    override fun detailsFor(code: AuthorizationCode): AuthorizationCodeDetails =
        inFlightCodes[code]?.also { inFlightCodes -= code } ?: error("code not stored")

    override fun create(request: Request, authRequest: AuthRequest, response: Response) =
        Success(AuthorizationCode(UUID.randomUUID().toString()).also {
            inFlightCodes[it] = AuthorizationCodeDetails(
                authRequest.client, authRequest.redirectUri!!, clock.instant().plus(1, ChronoUnit.DAYS), null, false,
                authRequest.responseType
            )
        })
}

private class InMemoryAuthRequestTracking : AuthRequestTracking {
    private val inFlightRequests = mutableListOf<AuthRequest>()

    override fun trackAuthRequest(request: Request, authRequest: AuthRequest, response: Response) =
        response.also { inFlightRequests += authRequest }

    override fun resolveAuthRequest(request: Request) =
        try {
            with(OAuthServer) {
                val extracted = AuthRequest(
                    clientIdQueryParameter(request),
                    scopesQueryParameter(request) ?: listOf(),
                    redirectUriQueryParameter(request),
                    state(request),
                    responseType(request)
                )
                if (inFlightRequests.remove(extracted)) extracted else null
            }
        } catch (e: LensFailure) {
            null
        }
}

private class SimpleAccessTokens() : AccessTokens {
    override fun create(clientId: ClientId, tokenRequest: TokenRequest) =
        Failure(UnsupportedGrantType("client_credentials"))

    override fun create(
        clientId: ClientId,
        tokenRequest: AuthorizationCodeAccessTokenRequest,
        authorizationCode: AuthorizationCode
    ) = Success(AccessToken("OAUTH_" + authorizationCode.value.reversed()))
}
