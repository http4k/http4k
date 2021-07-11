package org.http4k.security.oauth.server

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.Filter
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.NoOp
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.core.body.form
import org.http4k.core.then
import org.http4k.filter.ClientFilters.Cookies
import org.http4k.filter.ClientFilters.FollowRedirects
import org.http4k.filter.debug
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test

class OAuthServerTest {

    @Test
    fun `can follow authorization code flow`() {
        val authenticationServer = customOauthAuthorizationServer()
        val consumerApp = oauthClientApp(authenticationServer)

        val browser = Cookies().then(authenticationServer + consumerApp).debug()

        val browserWithRedirection = FollowRedirects().then(browser)

        val preAuthResponse = browser(Request(GET, "/a-protected-resource"))
        val loginPage = preAuthResponse.header("location")!!

        val loginPageResponse = browser(Request(GET, loginPage))
        assertThat(loginPageResponse, hasStatus(OK) and hasBody("Please authenticate"))

        val postAuthResponse = browserWithRedirection(Request(POST, loginPage).form("some", "credentials"))
        assertThat(postAuthResponse, hasStatus(OK) and hasBody("user resource"))
    }

    @Test
    fun `authorization flow with oauth request persistence`() {
        val authenticationServer = customOauthAuthorizationServerWithPersistence()
        val consumerApp = oauthClientApp(authenticationServer)

        val browser = Filter.NoOp
            .then(Cookies())
            .then(authenticationServer + consumerApp)

        val browserWithRedirection = FollowRedirects().then(browser)

        val preAuthResponse = browser(Request(GET, "/a-protected-resource"))
        val loginPage = preAuthResponse.header("location")!!

        val loginPageResponse = browser(Request(GET, loginPage))
        assertThat(loginPageResponse, hasStatus(OK) and hasBody("Please authenticate"))

        val postAuthResponse = browser(Request(POST, loginPage).form("some", "credentials"))
        val verifyPage = postAuthResponse.header("location")!!

        val verifyPageResponse = browser(Request(GET, verifyPage))
        assertThat(verifyPageResponse, hasStatus(OK) and hasBody("Allow my-app to access name and age?"))

        val postConfirmationResponse = browserWithRedirection(Request(POST, verifyPage))
        assertThat(postConfirmationResponse, hasStatus(OK) and hasBody("user resource"))
    }
}
