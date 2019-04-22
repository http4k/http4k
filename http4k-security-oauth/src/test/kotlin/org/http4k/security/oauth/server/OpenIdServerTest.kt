package org.http4k.security.oauth.server

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Filter
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.NoOp
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.core.body.form
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.http4k.security.ResponseType.CodeIdToken
import org.http4k.security.openid.IdTokenContainer
import org.junit.jupiter.api.Test


class OpenIdServerTest {

    private val debug = true

    @Test
    fun `can follow authorization code id_token flow`() {
        val authenticationServer = customOauthAuthorizationServer()
        val tokenConsumer = InMemoryIdTokenConsumer()
        val consumerApp = oauthClientApp(authenticationServer, debug, CodeIdToken, tokenConsumer)

        val browser = Filter.NoOp
            .then(ClientFilters.Cookies())
            .then(debugFilter(debug))
            .then(authenticationServer + consumerApp)

        val browserWithRedirection = ClientFilters.FollowRedirects().then(browser)

        val preAuthResponse = browser(Request(GET, "/a-protected-resource"))
        val loginPage = preAuthResponse.header("location")!!

        val loginPageResponse = browser(Request(GET, loginPage))
        assertThat(loginPageResponse, hasStatus(OK) and hasBody("Please authenticate"))

        val postAuthResponse = browserWithRedirection(Request(POST, loginPage).form("some", "credentials"))
        assertThat(postAuthResponse, hasStatus(OK) and hasBody("user resource"))

        assertThat(tokenConsumer.consumedFromAuthorizationResponse, equalTo(IdTokenContainer("dummy-id-token-for-unknown")))
        assertThat(tokenConsumer.consumedFromAccessTokenResponse, equalTo(IdTokenContainer("dummy-id-token-for-access-token")))
    }

}
