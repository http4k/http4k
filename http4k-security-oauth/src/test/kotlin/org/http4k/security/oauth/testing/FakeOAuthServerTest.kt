package org.http4k.security.oauth.testing

import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.Credentials
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters.Cookies
import org.http4k.filter.ClientFilters.FollowRedirects
import org.http4k.filter.debug
import org.http4k.hamkrest.hasBody
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.security.InsecureCookieBasedOAuthPersistence
import org.http4k.security.OAuthProvider
import org.http4k.security.OAuthProviderConfig
import org.junit.jupiter.api.Test

class FakeOAuthServerTest {
    @Test
    fun `auto logs in`() {
        val authPath = "/auth"
        val tokenPath = "/token"
        val callbackPath = "/cb"
        val protectedPath = "/getit"

        val oauth = FakeOAuthServer(authPath, tokenPath)
        val app = App(authPath, tokenPath, oauth, callbackPath, protectedPath)

        val browser = FollowRedirects()
            .then(Cookies())
            .then { r ->
                when (r.uri.host) {
                    "app" -> app
                    else -> oauth
                }(r)
            }

        assertThat(
            browser(
                Request(GET, "http://app$protectedPath")
            ), hasBody("LOGGEDIN")
        )
    }

    private fun App(
        authPath: String,
        tokenPath: String,
        oauth: HttpHandler,
        callbackPath: String,
        protectedPath: String
    ): RoutingHttpHandler {
        val oauthProvider = OAuthProvider(
            OAuthProviderConfig(
                Uri.of("http://auth"),
                authPath, tokenPath,
                Credentials("", "")
            ),
            oauth,
            Uri.of("http://app$callbackPath"),
            listOf(),
            InsecureCookieBasedOAuthPersistence("oauth")
        )

        return routes(
            callbackPath bind GET to oauthProvider.callback,
            protectedPath bind GET to oauthProvider.authFilter.then { Response(OK).body("LOGGEDIN") }
        )
    }
}
