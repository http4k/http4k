package org.http4k.security

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.http4k.core.Credentials
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.TEMPORARY_REDIRECT
import org.http4k.core.Uri
import org.http4k.core.cookie.cookie
import org.http4k.core.query
import org.http4k.core.then
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasStatus
import org.http4k.hamkrest.hasStatusDescription
import org.http4k.security.ResponseType.CodeIdToken
import org.http4k.security.openid.IdToken
import org.http4k.security.openid.IdTokenConsumer
import org.http4k.security.openid.RequestJwtContainer
import org.http4k.security.openid.RequestJwts
import org.junit.jupiter.api.Test

class OAuthProviderTest {
    private val providerConfig = OAuthProviderConfig(
        Uri.of("http://authHost/base"),
        "/auth",
        "/token",
        Credentials("user", "password"),
        Uri.of("http://apiHost/api/")
    )

    private val oAuthPersistence = FakeOAuthPersistence()

    private fun oAuth(
        persistence: OAuthPersistence,
        status: Status = OK,
        responseType: ResponseType = ResponseType.Code,
        nonceFromIdToken: Nonce? = null,
        resultIdTokenFromAuth: Result<Unit, OAuthCallbackError.InvalidIdToken> = Success(Unit),
        resultIdTokenFromAccessToken: Result<Unit, OAuthCallbackError.InvalidIdToken> = Success(Unit)
    ): OAuthProvider = OAuthProvider(
        providerConfig,
        { Response(status).body("access token goes here").header("request-uri", it.uri.toString()) },
        Uri.of("http://callbackHost/callback"),
        listOf("scope1", "scope2"),
        persistence,
        { it.query("response_mode", "form_post") },
        { CrossSiteRequestForgeryToken("randomCsrf") },
        { Nonce("randomNonce") },
        responseType,
        idTokenConsumer = object : IdTokenConsumer {
            override fun nonceFromIdToken(idToken: IdToken) = nonceFromIdToken
            override fun consumeFromAuthorizationResponse(idToken: IdToken) = resultIdTokenFromAuth
            override fun consumeFromAccessTokenResponse(idToken: IdToken) = resultIdTokenFromAccessToken
        }
    )

    @Test
    fun `filter - when accessToken value is present, request is let through`() {
        oAuthPersistence.assignToken(Request(GET, ""), Response(OK), AccessToken("randomToken"))
        assertThat(oAuth(oAuthPersistence).authFilter.then { Response(OK).body("i am witorious!") }(Request(GET, "/")), hasStatus(OK).and(hasBody("i am witorious!")))
    }

    @Test
    fun `filter - when no accessToken value present, request is redirected to expected location`() {
        val expectedHeader = """http://authHost/base/auth?client_id=user&response_type=code&scope=scope1+scope2&redirect_uri=http%3A%2F%2FcallbackHost%2Fcallback&state=randomCsrf&response_mode=form_post"""
        assertThat(oAuth(oAuthPersistence).authFilter.then { Response(OK) }(Request(GET, "/")), hasStatus(TEMPORARY_REDIRECT).and(hasHeader("Location", expectedHeader)))
    }

    @Test
    fun `filter - accepts custom request JWT container`() {
        val expectedHeader = """http://authHost/base/auth?client_id=user&response_type=code&scope=scope1+scope2&redirect_uri=http%3A%2F%2FcallbackHost%2Fcallback&state=randomCsrf&request=myCustomJwt&response_mode=form_post"""

        val jwts = RequestJwts { _, _, _ -> RequestJwtContainer("myCustomJwt") }
        assertThat(oAuth(oAuthPersistence).authFilter(jwts).then { Response(OK) }(Request(GET, "/")), hasStatus(TEMPORARY_REDIRECT).and(hasHeader("Location", expectedHeader)))
    }

    @Test
    fun `filter - request redirecttion may use other response_type`() {
        assertThat(oAuth(oAuthPersistence, OK, CodeIdToken)
            .authFilter.then { Response(OK) }(Request(GET, "/")), hasStatus(TEMPORARY_REDIRECT).and(hasHeader("Location", ".*response_type=code\\+id_token.*".toRegex())))
    }

    private val base = Request(GET, "/")
    private val withCookie = Request(GET, "/").cookie("serviceCsrf", "randomCsrf")
    private val withCode = withCookie.query("code", "value")
    private val withCodeAndInvalidState = withCode.query("state", "notreal")
    private val withCodeAndValidState = withCode.query("state", "randomCsrf")

    @Test
    fun `callback - when invalid inputs passed, we get forbidden with cookie invalidation`() {
        assertThat(oAuth(oAuthPersistence).callback(base), hasStatus(FORBIDDEN) and hasStatusDescription("Authorization code missing"))

        assertThat(oAuth(oAuthPersistence).callback(withCookie), hasStatus(FORBIDDEN) and hasStatusDescription("Authorization code missing"))

        assertThat(oAuth(oAuthPersistence).callback(withCodeAndInvalidState), hasStatus(FORBIDDEN) and hasStatusDescription("Invalid state (expected: null, received: notreal)"))
    }

    @Test
    fun `when api returns bad status`() {
        oAuthPersistence.assignCsrf(Response(OK), CrossSiteRequestForgeryToken("randomCsrf"))
        assertThat(oAuth(oAuthPersistence, INTERNAL_SERVER_ERROR).callback(withCodeAndValidState), hasStatus(FORBIDDEN) and hasStatusDescription("Failed to fetch access token (status: 500 Internal Server Error)"))
    }

    @Test
    fun `callback - when valid inputs passed, defaults value stored in oauth persistance`() {

        oAuthPersistence.assignCsrf(Response(OK), CrossSiteRequestForgeryToken("randomCsrf"))
        oAuthPersistence.assignOriginalUri(Response(OK), Uri.of("/defaulted"))

        val validRedirectToRoot = Response(TEMPORARY_REDIRECT)
            .header("Location", "/defaulted")
            .header("action", "assignToken")

        assertThat(oAuth(oAuthPersistence).callback(withCodeAndValidState), equalTo(validRedirectToRoot))
    }

    @Test
    fun `callback - when valid inputs passed, defaults to root if no uri is stored in oauth persistance`() {

        oAuthPersistence.assignCsrf(Response(OK), CrossSiteRequestForgeryToken("randomCsrf"))

        val validRedirectToRoot = Response(TEMPORARY_REDIRECT)
            .header("Location", "/")
            .header("action", "assignToken")

        assertThat(oAuth(oAuthPersistence).callback(withCodeAndValidState), equalTo(validRedirectToRoot))
    }

    @Test
    fun `api - uses base api uri`() {
        val response = oAuth(oAuthPersistence).api(Request(GET, "/some-resource"))

        assertThat(response, hasHeader("request-uri", equalTo("http://apiHost/api/some-resource")))
    }

    @Test
    fun `id token - can fail from id_token from callback request`() {
        val oauth = oAuth(oAuthPersistence, responseType = CodeIdToken, resultIdTokenFromAuth = Failure(OAuthCallbackError.InvalidIdToken("some reason")))

        assertThat(oauth.callback(withCodeAndValidState), hasStatus(FORBIDDEN))
    }

    @Test
    fun `id token - can fail from id_token from access token response`() {
        val oauth = oAuth(oAuthPersistence, responseType = CodeIdToken, resultIdTokenFromAccessToken = Failure(OAuthCallbackError.InvalidIdToken("some reason")))
        assertThat(oauth.callback(withCodeAndValidState), hasStatus(FORBIDDEN))
    }
}
