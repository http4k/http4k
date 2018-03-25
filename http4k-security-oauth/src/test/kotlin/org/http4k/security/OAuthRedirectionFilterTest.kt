package org.http4k.security

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.core.Credentials
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.TEMPORARY_REDIRECT
import org.http4k.core.Uri
import org.http4k.core.cookie.cookie
import org.http4k.core.then
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasStatus
import org.junit.Test
import java.time.Clock.fixed
import java.time.Instant.EPOCH
import java.time.ZoneId

class OAuthRedirectionFilterTest {
    private val clientConfig = OAuthConfig(
        "service",
        Uri.of("http://authHost"),
        "/auth",
        "/token",
        Credentials("user", "password"),
        Uri.of("http://apiHost")
    )

    private val filter = OAuthRedirectionFilter(
        clientConfig,
        "csrf",
        "accessToken",
        Uri.of("http://callbackHost/callback"),
        listOf("scope1", "scope2"),
        fixed(EPOCH, ZoneId.of("GMT")),
        { "randomCsrf" },
        { "randomNonce" }
    )

    private val app = filter.then { Response(OK) }

    @Test
    fun `when accessToken cookie is passed, request is let through`() {
        app(Request(GET, "/").cookie("accessToken", "some value")) shouldMatch hasStatus(OK)
    }

    @Test
    fun `when no accessToken cookie is passed, request is redirected to expected location`() {
        val expectedHeader = """http://authHost/auth?client_id=user&response_type=code&scope=scope1+scope2&redirect_uri=http%3A%2F%2FcallbackHost%2Fcallback&state=csrf%3DrandomCsrf%26uri%3D%252F&nonce=randomNonce"""
        app(Request(GET, "/")) shouldMatch hasStatus(TEMPORARY_REDIRECT).and(hasHeader("Location", expectedHeader))
    }
}