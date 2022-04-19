package org.http4k.security

import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.Credentials
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ServerFilters
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test

class BasicAuthAccessTokenFetcherAuthenticatorTest {
    val credentials = Credentials("user", "pass")
    val provider = OAuthProviderConfig(Uri.of("irrelevant"), "/", "/path", credentials)

    @Test
    fun `sends credentials as expected`() {
        val authenticator = BasicAuthAccessTokenFetcherAuthenticator(provider)
        val server = ServerFilters.BasicAuth("irrelevant", credentials).then { Response(OK) }

        assertThat(server(authenticator.authenticate(Request(Method.GET, "/"))), hasStatus(OK))
    }

    @Test
    fun `invalid credentials`() {
        val authenticator = BasicAuthAccessTokenFetcherAuthenticator(provider)
        val server = ServerFilters.BasicAuth("irrelevant", Credentials("something", "else")).then { Response(OK) }

        assertThat(server(authenticator.authenticate(Request(Method.GET, "/"))), hasStatus(UNAUTHORIZED))
    }
}
