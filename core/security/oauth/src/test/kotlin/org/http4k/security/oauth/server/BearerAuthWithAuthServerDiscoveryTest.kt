package org.http4k.security.oauth.server

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ServerFilters
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test

class BearerAuthWithAuthServerDiscoveryTest {

    private val authServerUri = Uri.of("https://auth.example.com")
    private val realm = "test-realm"
    private val validToken = "valid-token"
    private val successHandler = { _: Request -> Response(OK).body("success") }

    @Test
    fun `passes request through when valid token is provided`() {
        val filter = ServerFilters.BearerAuthWithAuthServerDiscovery(realm, authServerUri) { token ->
            token == validToken
        }

        val response = filter.then(successHandler)(
            Request(GET, "/protected")
                .header("Authorization", "Bearer $validToken")
        )

        assertThat(response, hasStatus(OK).and(hasBody("success")))
    }

    @Test
    fun `returns 401 when no token is provided`() {
        val filter = ServerFilters.BearerAuthWithAuthServerDiscovery(realm, authServerUri) { true }

        val response = filter.then(successHandler)(
            Request(GET, "/protected")
        )

        assertThat(response, hasStatus(UNAUTHORIZED))
        assertThat(
            response,
            hasHeader(
                "WWW-Authenticate",
                equalTo("""Bearer realm="$realm", error="invalid_token", auth_server="$authServerUri" """)
            )
        )
    }

    @Test
    fun `returns 401 when invalid token is provided`() {
        val filter = ServerFilters.BearerAuthWithAuthServerDiscovery(realm, authServerUri) { token ->
            token == validToken
        }

        val response = filter.then(successHandler)(
            Request(GET, "/protected")
                .header("Authorization", "Bearer invalid-token")
        )

        assertThat(response, hasStatus(UNAUTHORIZED))
        assertThat(
            response,
            hasHeader(
                "WWW-Authenticate",
                equalTo("""Bearer realm="$realm", error="invalid_token", auth_server="$authServerUri" """)
            )
        )
    }

    @Test
    fun `returns 401 when token has wrong scheme`() {
        val filter = ServerFilters.BearerAuthWithAuthServerDiscovery(realm, authServerUri) { token ->
            token == validToken
        }

        val response = filter.then(successHandler)(
            Request(GET, "/protected")
                .header("Authorization", "Basic $validToken")
        )

        assertThat(response, hasStatus(UNAUTHORIZED))
        assertThat(
            response,
            hasHeader(
                "WWW-Authenticate",
                equalTo("""Bearer realm="$realm", error="invalid_token", auth_server="$authServerUri" """)
            )
        )
    }
}
