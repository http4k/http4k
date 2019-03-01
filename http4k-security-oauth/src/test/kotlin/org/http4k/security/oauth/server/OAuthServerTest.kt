package org.http4k.security.oauth.server

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.Credentials
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.NoOp
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.body.form
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.DebuggingFilters
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.security.InsecureCookieBasedOAuthPersistence
import org.http4k.security.OAuthProvider
import org.http4k.security.OAuthProviderConfig
import org.http4k.util.FixedClock
import org.junit.jupiter.api.Test


class OAuthServerTest {

    private val debug = true

    @Test
    fun `can follow authorization code flow`() {
        val authenticationServer = customOauthAuthorizationServer()
        val consumerApp = oauthClientApp(authenticationServer, debug)

        val browser = Filter.NoOp.then(debugFilter(debug))
            .then(ClientFilters.Cookies())
            .then(authenticationServer + consumerApp)

        val browserWithRedirection = ClientFilters.FollowRedirects().then(browser)

        val preAuthResponse = browser(Request(GET, "/a-protected-resource"))
        val loginPage = preAuthResponse.header("location")!!

        val loginPageResponse = browser(Request(GET, loginPage))
        assertThat(loginPageResponse, hasStatus(OK) and hasBody("Please authenticate"))

        val postAuthResponse = browserWithRedirection(Request(POST, loginPage).form("some", "credentials"))
        assertThat(postAuthResponse, hasStatus(OK) and hasBody("user resource"))
    }

    private fun customOauthAuthorizationServer(): RoutingHttpHandler {
        val server = OAuthServer(
            tokenPath = "/oauth2/token",
            clientValidator = DummyClientValidator(),
            authorizationCodes = InMemoryAuthorizationCodes(FixedClock),
            accessTokens = DummyAccessTokens(),
            clock = FixedClock
        )

        return routes(
            server.tokenRoute,
            "/my-login-page" bind GET to server.authenticationStart.then { Response(OK).body("Please authenticate") },
            "/my-login-page" bind POST to server.authenticationComplete.then { Response(OK) }
        )
    }

    private fun oauthClientApp(tokenClient: HttpHandler, debug: Boolean): RoutingHttpHandler {
        val persistence = InsecureCookieBasedOAuthPersistence("oauthTest")

        val oauthProvider = OAuthProvider(
            OAuthProviderConfig(Uri.of("http://irrelevant"),
                "/my-login-page", "/oauth2/token",
                Credentials("username", "somepassword"),
                Uri.of("https://irrelevant")),
            debugFilter(debug).then(tokenClient),
            Uri.of("/my-callback"),
            listOf("nameScope", "familyScope"),
            persistence
        )

        return routes(
            "/my-callback" bind GET to oauthProvider.callback,
            "/a-protected-resource" bind GET to oauthProvider.authFilter.then { Response(Status.OK).body("user resource") }
        )
    }
}

private fun debugFilter(active: Boolean) = Filter.switchable(active, DebuggingFilters.PrintRequestAndResponse())
private fun Filter.Companion.switchable(active: Boolean, next: Filter) = if (active) next else Filter.NoOp
private operator fun RoutingHttpHandler.plus(other: RoutingHttpHandler) = { request: Request -> routes(this, other)(request) }
