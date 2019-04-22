package org.http4k.security.oauth.server

import org.http4k.core.*
import org.http4k.filter.DebuggingFilters
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.security.InsecureCookieBasedOAuthPersistence
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
            clock = FixedClock
    )

    return routes(
            server.tokenRoute,
            "/my-login-page" bind Method.GET to server.authenticationStart.then { Response(Status.OK).body("Please authenticate") },
            "/my-login-page" bind Method.POST to server.authenticationComplete.then { Response(Status.OK) }
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
            clock = FixedClock
    )

    return routes(
            server.tokenRoute,
            "/my-login-page" bind Method.GET to server.authenticationStart.then { Response(Status.OK).body("Please authenticate") },
            "/my-login-page" bind Method.POST to { Response(Status.TEMPORARY_REDIRECT).header("location", "/verify-scope") },
            "/verify-scope" bind Method.GET to {
                val flow = requestPersistence.resolveAuthRequest(it) ?: error("flow was not persisted")
                Response(Status.OK).body("Allow ${flow.client.value} to access ${flow.scopes.joinToString(" and ")}?")
            },
            "/verify-scope" bind Method.POST to server.authenticationComplete.then { Response(Status.OK) }
    )
}

fun oauthClientApp(
        tokenClient: HttpHandler,
        debug: Boolean,
        responseType: ResponseType = ResponseType.Code,
        idTokenConsumer: IdTokenConsumer = IdTokenConsumer.NoOp
): RoutingHttpHandler {
    val persistence = InsecureCookieBasedOAuthPersistence("oauthTest")

    val oauthProvider = OAuthProvider(
            OAuthProviderConfig(Uri.of("http://irrelevant"),
                    "/my-login-page", "/oauth2/token",
                    Credentials("my-app", "somepassword"),
                    Uri.of("https://irrelevant")),
            debugFilter(debug).then(tokenClient),
            Uri.of("/my-callback"),
            listOf("name", "age"),
            persistence,
            responseType = responseType,
            idTokenConsumer = idTokenConsumer
    )

    return routes(
            "/my-callback" bind Method.GET to oauthProvider.callback,
            "/a-protected-resource" bind Method.GET to oauthProvider.authFilter.then { Response(Status.OK).body("user resource") }
    )
}

fun debugFilter(active: Boolean) = Filter.switchable(active, DebuggingFilters.PrintRequestAndResponse())
private fun Filter.Companion.switchable(active: Boolean, next: Filter) = if (active) next else Filter.NoOp

operator fun RoutingHttpHandler.plus(other: RoutingHttpHandler) = { request: Request -> routes(this, other)(request) }
