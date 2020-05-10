package org.http4k.security.oauth.server

import org.http4k.core.Credentials
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.TEMPORARY_REDIRECT
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.format.Jackson
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.security.InsecureCookieBasedOAuthPersistence
import org.http4k.security.OAuthPersistence
import org.http4k.security.OAuthProvider
import org.http4k.security.OAuthProviderConfig
import org.http4k.security.ResponseType
import org.http4k.security.openid.IdTokenConsumer
import org.http4k.util.FixedClock

fun customOauthAuthorizationServer(): RoutingHttpHandler {
    val server = OAuthServer(
        tokenPath = "/oauth2/token",
        authRequestTracking = DummyOAuthAuthRequestTracking(),
        clientValidator = DummyClientValidator(),
        authorizationCodes = InMemoryAuthorizationCodes(FixedClock),
        accessTokens = DummyAccessTokens(),
        json = Jackson,
        clock = FixedClock,
        authRequestExtractor = AuthRequestFromQueryParameters,
        idTokens = DummyIdTokens()
    )

    return routes(
        server.tokenRoute,
        "/my-login-page" bind GET to server.authenticationStart.then { Response(OK).body("Please authenticate") },
        "/my-login-page" bind POST to server.authenticationComplete
    )
}

fun customOauthAuthorizationServerWithPersistence(): RoutingHttpHandler {
    val requestPersistence = InsecureCookieBasedAuthRequestTracking()

    val server = OAuthServer(
        tokenPath = "/oauth2/token",
        authRequestTracking = requestPersistence,
        clientValidator = DummyClientValidator(),
        authorizationCodes = InMemoryAuthorizationCodes(FixedClock),
        accessTokens = DummyAccessTokens(),
        json = Jackson,
        clock = FixedClock,
        authRequestExtractor = AuthRequestFromQueryParameters
    )

    return routes(
        server.tokenRoute,
        "/my-login-page" bind GET to server.authenticationStart.then { Response(OK).body("Please authenticate") },
        "/my-login-page" bind POST to { Response(TEMPORARY_REDIRECT).header("location", "/verify-scope") },
        "/verify-scope" bind GET to {
            val flow = requestPersistence.resolveAuthRequest(it) ?: error("flow was not persisted")
            Response(OK).body("Allow ${flow.client.value} to access ${flow.scopes.joinToString(" and ")}?")
        },
        "/verify-scope" bind POST to server.authenticationComplete
    )
}

fun oauthClientApp(
    tokenClient: HttpHandler,
    responseType: ResponseType = ResponseType.Code,
    idTokenConsumer: IdTokenConsumer = IdTokenConsumer.NoOp,
    scopes: List<String> = listOf("name", "age"),
    persistence: OAuthPersistence = InsecureCookieBasedOAuthPersistence("oauthTest")
): RoutingHttpHandler {

    val oauthProvider = OAuthProvider(
        OAuthProviderConfig(Uri.of("http://irrelevant"),
            "/my-login-page", "/oauth2/token",
            Credentials("my-app", "somepassword"),
            Uri.of("https://irrelevant")),
        tokenClient,
        Uri.of("/my-callback"),
        scopes,
        persistence,
        responseType = responseType,
        idTokenConsumer = idTokenConsumer
    )

    return routes(
        "/my-callback" bind GET to oauthProvider.callback,
        "/a-protected-resource" bind GET to oauthProvider.authFilter.then { Response(OK).body("user resource") }
    )
}

operator fun RoutingHttpHandler.plus(other: RoutingHttpHandler) = { request: Request -> routes(this, other)(request) }
