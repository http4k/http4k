package org.http4k.security.oauth.server

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.*
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test

internal class AccessTokenValidationFilterTest {
    private val token: String = "badger"
    private val nextPage = { _: Request -> Response(Status.OK).body("some page") }
    private val isNextPage = hasStatus(Status.OK) and hasBody("some page")
    private val validTokensFilter = AccessTokenValidationFilter(DummyAccessTokens()).then(nextPage)
    private val invalidTokensFilter = AccessTokenValidationFilter(DummyAccessTokens(false)).then(nextPage)

    @Test
    internal fun `returns unauthorized for no authorization header`() {
        val response = validTokensFilter(Request(Method.GET, "something"))
        assertThat(response, hasStatus(Status.UNAUTHORIZED));
    }

    @Test
    internal fun `returns unauthorized for authorization header which is a not a bearer token`() {
        val response = validTokensFilter(Request(Method.GET, "something").header("Authorization", "Basic ${token}"))
        assertThat(response, hasStatus(Status.UNAUTHORIZED))
    }

    @Test
    internal fun `returns unauthorized for access token which is not valid`() {
        val response = invalidTokensFilter(Request(Method.GET, "something").header("Authorization", "Bearer ${token}"))
        assertThat(response, hasStatus(Status.UNAUTHORIZED))
    }

    @Test
    internal fun `proceeds to next filter if access token is present and valid`() {
        val response = validTokensFilter(Request(Method.GET, "something").header("Authorization", "Bearer ${token}"))
        assertThat(response, isNextPage)
    }

    @Test
    internal fun `proceeds to next filter if access token is present and valid ignoring random spacing in the header`() {
        val response = validTokensFilter(Request(Method.GET, "something").header("Authorization", "     Bearer    ${token}     "))
        assertThat(response, isNextPage)
    }
}