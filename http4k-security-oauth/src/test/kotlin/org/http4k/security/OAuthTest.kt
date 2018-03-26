package org.http4k.security

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.core.Credentials
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.I_M_A_TEAPOT
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.TEMPORARY_REDIRECT
import org.http4k.core.Uri
import org.http4k.core.cookie.cookie
import org.http4k.core.query
import org.http4k.core.then
import org.http4k.core.toUrlFormEncoded
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasStatus
import org.junit.Test

class FakeOAuthPersistence : OAuthPersistence {

    var csrf: String? = null
    var accessToken: String? = null

    override fun retrieveCsrf(p1: Request): String? = csrf

    override fun assignCsrf(redirect: Response, csrf: String): Response {
        this.csrf = csrf
        return redirect.header("action", "assignCsrf")
    }

    override fun retrieveToken(p1: Request): String? = accessToken

    override fun assignToken(redirect: Response, accessToken: String): Response {
        this.accessToken = accessToken
        return redirect.header("action", "assignToken")
    }

    override fun authFailureResponse() = Response(I_M_A_TEAPOT)
}

class OAuthTest {
    private val clientConfig = OAuthConfig(
        Uri.of("http://authHost"),
        "/auth",
        "/token",
        Credentials("user", "password"),
        Uri.of("http://apiHost")
    )

    private val oAuthPersistence = FakeOAuthPersistence()

    private fun oAuth(persistence: OAuthPersistence): OAuth = OAuth(
        { Response(OK).body("access token goes here") },
        clientConfig, Uri.of("http://callbackHost/callback"),
        listOf("scope1", "scope2"),
        persistence,
        { it.query("nonce", "randomNonce") },
        { "randomCsrf" }
    )

    @Test
    fun `filter - when accessToken value is present, request is let through`() {
        oAuthPersistence.assignToken(Response(OK), "randomToken")
        oAuth(oAuthPersistence).authFilter.then { Response(OK).body("i am witorious!") }(Request(GET, "/")) shouldMatch
            hasStatus(OK).and(hasBody("i am witorious!"))
    }

    @Test
    fun `filter - when no accessToken value present, request is redirected to expected location`() {
        val expectedHeader = """http://authHost/auth?client_id=user&response_type=code&scope=scope1+scope2&redirect_uri=http%3A%2F%2FcallbackHost%2Fcallback&state=csrf%3DrandomCsrf%26uri%3D%252F&nonce=randomNonce"""
        Request(GET, "/")
        oAuth(oAuthPersistence).authFilter.then { Response(OK) }(Request(GET, "/")) shouldMatch hasStatus(TEMPORARY_REDIRECT).and(hasHeader("Location", expectedHeader))
    }

    private val base = Request(GET, "/")
    private val withCookie = Request(GET, "/").cookie("serviceCsrf", "randomCsrf")
    private val withCode = withCookie.query("code", "value")
    private val withCodeAndInvalidState = withCode.query("state", listOf("csrf" to "notreal").toUrlFormEncoded())
    private val withCodeAndValidStateButNoUrl = withCode.query("state", listOf("csrf" to "randomCsrf").toUrlFormEncoded())

    @Test
    fun `callback - when invalid inputs passed, we get forbidden with cookie invalidation`() {
        val invalidation = Response(I_M_A_TEAPOT)

        oAuth(oAuthPersistence).callback(base) shouldMatch equalTo(invalidation)

        oAuth(oAuthPersistence).callback(withCookie) shouldMatch equalTo(invalidation)

        oAuth(oAuthPersistence).callback(withCode) shouldMatch equalTo(invalidation)

        oAuth(oAuthPersistence).callback(withCodeAndInvalidState) shouldMatch equalTo(invalidation)
    }

    @Test
    fun `callback - when valid inputs passed, defaults to root`() {

        oAuthPersistence.assignCsrf(Response(OK), "randomCsrf")

        val validRedirectToRoot = Response(TEMPORARY_REDIRECT)
            .header("Location", "/")
            .header("action", "assignToken")

        oAuth(oAuthPersistence).callback(withCodeAndValidStateButNoUrl) shouldMatch equalTo(validRedirectToRoot)
    }

}