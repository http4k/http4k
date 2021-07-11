package org.http4k.security.oauth.server

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.body.form
import org.http4k.core.cookie.cookie
import org.http4k.core.cookie.cookies
import org.http4k.core.findSingle
import org.http4k.core.queries
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.http4k.security.InsecureCookieBasedOAuthPersistence
import org.http4k.security.Nonce
import org.http4k.security.ResponseType.CodeIdToken
import org.http4k.security.openid.IdToken
import org.junit.jupiter.api.Test

class OpenIdServerTest {

    @Test
    fun `can follow authorization code id_token flow`() {
        val clientOauthPersistence = InsecureCookieBasedOAuthPersistence("oauthTest")
        val authenticationServer = customOauthAuthorizationServer()
        val tokenConsumer = InMemoryIdTokenConsumer()
        val consumerApp = oauthClientApp(
            authenticationServer,
            CodeIdToken,
            tokenConsumer,
            listOf("openid", "name", "age"),
            clientOauthPersistence
        )

        val browser = ClientFilters.Cookies().then(authenticationServer + consumerApp)

        val browserWithRedirection = ClientFilters.FollowRedirects().then(browser)

        val preAuthResponse = browser(Request(GET, "/a-protected-resource"))
        val authRequestUri = preAuthResponse.header("location")!!

        val suppliedNonce = Uri.of(authRequestUri).queries().findSingle("nonce")?.let { Nonce(it) }
        val storedNonce = clientOauthPersistence
            .retrieveNonce(preAuthResponse.cookies()
                .fold(Request(GET, "/")) { acc, c -> acc.cookie(c) })
        assertThat(storedNonce, present())
        assertThat(suppliedNonce, present())
        assertThat(suppliedNonce, equalTo(storedNonce))
        tokenConsumer.expectedNonce = suppliedNonce

        val loginPageResponse = browser(Request(GET, authRequestUri))
        assertThat(loginPageResponse, hasStatus(OK) and hasBody("Please authenticate"))

        val postAuthResponse = browserWithRedirection(Request(POST, authRequestUri).form("some", "credentials"))
        assertThat(postAuthResponse, hasStatus(OK) and hasBody("user resource"))

        assertThat(
            tokenConsumer.consumedFromAuthorizationResponse,
            equalTo(IdToken("dummy-id-token-for-unknown-nonce:${suppliedNonce?.value}"))
        )
        assertThat(tokenConsumer.consumedFromAccessTokenResponse, equalTo(IdToken("dummy-id-token-for-access-token")))
    }

    @Test
    fun `reject oidc flow if nonces do not match`() {
        val clientOauthPersistence = InsecureCookieBasedOAuthPersistence("oauthTest")
        val authenticationServer = customOauthAuthorizationServer()
        val tokenConsumer = InMemoryIdTokenConsumer(expectedNonce = Nonce("some invalid nonce"))
        val consumerApp = oauthClientApp(
            authenticationServer,
            CodeIdToken,
            tokenConsumer,
            listOf("openid", "name", "age"),
            clientOauthPersistence
        )

        val browser = ClientFilters.Cookies().then(authenticationServer + consumerApp)

        val browserWithRedirection = ClientFilters.FollowRedirects().then(browser)

        val preAuthResponse = browser(Request(GET, "/a-protected-resource"))
        val authRequestUri = preAuthResponse.header("location")!!

        val loginPageResponse = browser(Request(GET, authRequestUri))
        assertThat(loginPageResponse, hasStatus(OK) and hasBody("Please authenticate"))

        val postAuthResponse = browserWithRedirection(Request(POST, authRequestUri).form("some", "credentials"))
        assertThat(postAuthResponse, hasStatus(FORBIDDEN))
    }
}
