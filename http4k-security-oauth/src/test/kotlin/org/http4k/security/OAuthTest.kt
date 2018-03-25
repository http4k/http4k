package org.http4k.security

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.core.Credentials
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.TEMPORARY_REDIRECT
import org.http4k.core.Uri
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookie
import org.http4k.core.cookie.invalidateCookie
import org.http4k.core.query
import org.http4k.core.then
import org.http4k.core.toUrlFormEncoded
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasStatus
import org.junit.Test
import java.time.Clock.fixed
import java.time.Instant.EPOCH
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class OAuthTest {
    private val clock = fixed(EPOCH, ZoneId.of("GMT"))

    private val clientConfig = OAuthConfig(
        "service",
        Uri.of("http://authHost"),
        "/auth",
        "/token",
        Credentials("user", "password"),
        Uri.of("http://apiHost")
    )

    private val oauth = OAuth(
        { Response(OK).body("access token goes here") },
        clientConfig, Uri.of("http://callbackHost/callback"),
        listOf("scope1", "scope2"), clock, { "randomCsrf" },
        { it.query("nonce", "randomNonce") }
    )

    private val filter = oauth.authFilter.then { Response(OK) }

    @Test
    fun `filter - when accessToken cookie is passed, request is let through`() {
        filter(Request(GET, "/").cookie("serviceAccessToken", "some value")) shouldMatch hasStatus(OK)
    }

    @Test
    fun `filter - when no accessToken cookie is passed, request is redirected to expected location`() {
        val expectedHeader = """http://authHost/auth?client_id=user&response_type=code&scope=scope1+scope2&redirect_uri=http%3A%2F%2FcallbackHost%2Fcallback&state=serviceCsrf%3DrandomCsrf%26uri%3D%252F&nonce=randomNonce"""
        filter(Request(GET, "/")) shouldMatch hasStatus(TEMPORARY_REDIRECT).and(hasHeader("Location", expectedHeader))
    }

    private val base = Request(GET, "/")
    private val withCookie = Request(GET, "/").cookie("serviceCsrf", "randomCsrf")
    private val withCode = withCookie.query("code", "value")
    private val withCodeAndInvalidState = withCode.query("state", listOf("serviceCsrf" to "notreal").toUrlFormEncoded())
    private val withCodeAndValidStateButNoUrl = withCode.query("state", listOf("serviceCsrf" to "randomCsrf").toUrlFormEncoded())

    @Test
    fun `callback - when invalid inputs passed, we get forbidden with cookie invalidation`() {
        val invalidation = Response(FORBIDDEN).invalidateCookie("serviceCsrf").invalidateCookie("serviceAccessToken")

        oauth.callback(base) shouldMatch equalTo(invalidation)

        oauth.callback(withCookie) shouldMatch equalTo(invalidation)

        oauth.callback(withCode) shouldMatch equalTo(invalidation)

        oauth.callback(withCodeAndInvalidState) shouldMatch equalTo(invalidation)

    }

    @Test
    fun `callback - when valid inputs passed, defaults to root`() {
        val validRedirectToRoot = Response(TEMPORARY_REDIRECT)
            .header("Location", "/")
            .invalidateCookie("serviceCsrf")
            .cookie(Cookie("serviceAccessToken", "access token goes here", expires = LocalDateTime.ofEpochSecond(3600, 0, ZoneOffset.UTC)))

        oauth.callback(withCodeAndValidStateButNoUrl) shouldMatch equalTo(validRedirectToRoot)
    }

}