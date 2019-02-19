package org.http4k.security.oauth.server

import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.Credentials
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method
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
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.security.InsecureCookieBasedOAuthPersistence
import org.http4k.security.OAuthProvider
import org.http4k.security.OAuthProviderConfig
import org.junit.jupiter.api.Test


class OAuthServerTest {

    private val debug = false

    @Test
    fun `can follow authorization code flow`() {
        val server = OAuthServer(
                tokenPath = "/oauth2/token",
                validateClientAndRedirectionUri = { _, _ -> true },
                authorizationCodes = DummyAuthorizationCodes(),
                persistence = InsecureCookieBasedOAuthRequestPersistence()
        )

        val oauthConsumerApp = OauthClientApp(server, debug)

        val browser = Filter.NoOp
                .then(ClientFilters.FollowRedirects())
                .then(ClientFilters.Cookies())
                .then(debugFilter(debug))
                .then(oauthConsumerApp)

        val promptToAuthenticate = browser(Request(Method.GET, "/a-protected-resource"))
        assertThat(promptToAuthenticate, hasStatus(OK))
        assertThat(promptToAuthenticate, hasBody("Please authenticate"))

        val resource = browser(Request(Method.POST, "/my-login-page").form("some", "credentials"))
        assertThat(resource, hasStatus(OK))
        assertThat(resource, hasBody("user resource"))
    }

    private class OauthClientApp(oauthAuthorizationServer: OAuthServer, debug: Boolean) : HttpHandler {
        override fun invoke(request: Request): Response = app(request)

        val persistence = InsecureCookieBasedOAuthPersistence("oauthTest")

        val oauthProvider = OAuthProvider(
                OAuthProviderConfig(Uri.of("http://irrelevant"),
                        "/my-login-page", "/oauth2/token",
                        Credentials("username", "somepassword"),
                        Uri.of("https://irrelevant")),
                debugFilter(debug).then(oauthAuthorizationServer.tokenRoute),
                Uri.of("/my-callback"),
                listOf("nameScope", "familyScope"),
                persistence
        )

        val app = routes(
                oauthAuthorizationServer.tokenRoute,
                "/my-login-page" bind Method.GET to oauthAuthorizationServer.authenticationStart.then { Response(OK).body("Please authenticate") },
                "/my-login-page" bind Method.POST to oauthAuthorizationServer.authenticationComplete.then { Response(OK) },
                "/my-callback" bind Method.GET to oauthProvider.callback,
                "/a-protected-resource" bind Method.GET to oauthProvider.authFilter.then { Response(Status.OK).body("user resource") }
        )
    }

}

private fun debugFilter(active: Boolean) = Filter.switchable(active, DebuggingFilters.PrintRequestAndResponse())
private fun Filter.Companion.switchable(active: Boolean, next: Filter) = if (active) next else Filter.NoOp
