package org.http4k.security.oauth.server

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.core.*
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.body.form
import org.http4k.format.AutoMarshallingJson
import org.http4k.format.Jackson
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.http4k.security.AccessTokenResponse
import org.http4k.security.ResponseType.CodeIdToken
import org.http4k.security.accessTokenResponseBody
import org.http4k.security.oauth.server.AccessTokenCreationError.AUTHORIZATION_CODE_ALREADY_USED
import org.http4k.util.FixedClock
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit.SECONDS
import java.time.temporal.TemporalUnit

class GenerateAccessTokenTest {

    private val json: AutoMarshallingJson = Jackson
    private val handlerClock = SettableClock()
    private val codes = InMemoryAuthorizationCodes(FixedClock)
    private val authRequest = AuthRequest(ClientId("a-clientId"), listOf(), Uri.of("redirect"), "state")
    private val request = Request(Method.GET, "http://some-thing")
    private val code = codes.create(request, authRequest, Response(OK))
    private val handler = GenerateAccessToken(HardcodedClientValidator(authRequest.client, authRequest.redirectUri, "a-secret"), codes, DummyAccessTokens(), handlerClock, DummyIdtokens(), json)

    @Test
    fun `generates a dummy token`() {
        val response = handler(Request(Method.POST, "/token")
            .header("content-type", ContentType.APPLICATION_FORM_URLENCODED.value)
            .form("grant_type", "authorization_code")
            .form("code", code.value)
            .form("client_id", authRequest.client.value)
            .form("client_secret", "a-secret")
            .form("redirect_uri", authRequest.redirectUri.toString())
        )

        assertThat(response, hasStatus(OK) and hasBody("dummy-access-token"))
        assertThat(codes.available(code), equalTo(false))
    }

    @Test
    fun `generates dummy access_token and id_token`() {
        val codeForIdTokenRequest = codes.create(request, authRequest.copy(responseType = CodeIdToken), Response(OK))

        val response = handler(Request(Method.POST, "/token")
            .header("content-type", ContentType.APPLICATION_FORM_URLENCODED.value)
            .form("grant_type", "authorization_code")
            .form("code", codeForIdTokenRequest.value)
            .form("client_id", authRequest.client.value)
            .form("client_secret", "a-secret")
            .form("redirect_uri", authRequest.redirectUri.toString())
        )

        assertThat(response, hasStatus(OK))

        assertThat(accessTokenResponseBody(response), equalTo(AccessTokenResponse("dummy-access-token", "dummy-id-token-for-access-token")))
    }

    @Test
    fun `handles invalid grant_type`() {
        val response = handler(Request(Method.POST, "/token")
            .header("content-type", ContentType.APPLICATION_FORM_URLENCODED.value)
            .form("grant_type", "something_else")
            .form("code", code.value)
            .form("client_id", authRequest.client.value)
            .form("client_secret", "a-secret")
            .form("redirect_uri", authRequest.redirectUri.toString())
        )

        assertThat(response, hasStatus(BAD_REQUEST) and hasBody(withErrorType("unsupported_grant_type")))
    }

    @Test
    fun `handles invalid client credentials`() {
        val response = handler(Request(Method.POST, "/token")
            .header("content-type", ContentType.APPLICATION_FORM_URLENCODED.value)
            .form("grant_type", "authorization_code")
            .form("code", code.value)
            .form("client_id", authRequest.client.value)
            .form("client_secret", "wrong-secret")
            .form("redirect_uri", authRequest.redirectUri.toString())
        )

        assertThat(response, hasStatus(UNAUTHORIZED) and hasBody(withErrorType("invalid_client")))
    }

    @Test
    fun `handles expired code`() {
        handlerClock.advance(1, SECONDS)

        val expiredCode = codes.create(request, authRequest, Response(OK))

        val response = handler(Request(Method.POST, "/token")
            .header("content-type", ContentType.APPLICATION_FORM_URLENCODED.value)
            .form("grant_type", "authorization_code")
            .form("code", expiredCode.value)
            .form("client_id", authRequest.client.value)
            .form("client_secret", "a-secret")
            .form("redirect_uri", authRequest.redirectUri.toString())
        )

        assertThat(response, hasStatus(BAD_REQUEST) and hasBody(withErrorType("invalid_grant")))
    }

    @Test
    fun `handles client id different from one in authorization code`(){
        val storedCode = codes.create(request, authRequest.copy(client = ClientId("different client")), Response(OK))

        val response = handler(Request(Method.POST, "/token")
            .header("content-type", ContentType.APPLICATION_FORM_URLENCODED.value)
            .form("grant_type", "authorization_code")
            .form("code", storedCode.value)
            .form("client_id", authRequest.client.value)
            .form("client_secret", "a-secret")
            .form("redirect_uri", authRequest.redirectUri.toString())
        )

        assertThat(response, hasStatus(BAD_REQUEST) and hasBody(withErrorType("invalid_grant")))
    }

    @Test
    fun `handles redirectUri different from one in authorization code`(){
        val storedCode = codes.create(request, authRequest.copy(redirectUri = Uri.of("somethingelse")), Response(OK))

        val response = handler(Request(Method.POST, "/token")
            .header("content-type", ContentType.APPLICATION_FORM_URLENCODED.value)
            .form("grant_type", "authorization_code")
            .form("code", storedCode.value)
            .form("client_id", authRequest.client.value)
            .form("client_secret", "a-secret")
            .form("redirect_uri", authRequest.redirectUri.toString())
        )

        assertThat(response, hasStatus(BAD_REQUEST) and hasBody(withErrorType("invalid_grant")))
    }

    @Test
    fun `handles already used authentication code`(){
        val handler = GenerateAccessToken(HardcodedClientValidator(authRequest.client, authRequest.redirectUri, "a-secret"), codes, ErroringAccessTokens(AUTHORIZATION_CODE_ALREADY_USED), handlerClock, DummyIdtokens(), json)
        val request = Request(Method.POST, "/token")
                .header("content-type", ContentType.APPLICATION_FORM_URLENCODED.value)
                .form("grant_type", "authorization_code")
                .form("code", code.value)
                .form("client_id", authRequest.client.value)
                .form("client_secret", "a-secret")
                .form("redirect_uri", authRequest.redirectUri.toString())
        val response = handler(request)

        assertThat(response, hasStatus(BAD_REQUEST) and hasBody(withErrorType("invalid_grant")))
    }

    private fun withErrorType(errorType: String) = containsSubstring("\"error\":\"$errorType\"")
            .and(containsSubstring("\"error_description\":"))
}

class SettableClock : Clock() {
    private var currentTime = Instant.EPOCH

    fun advance(amount: Long, unit: TemporalUnit) {
        currentTime = currentTime.plus(amount, unit)
    }

    override fun withZone(zone: ZoneId?): Clock = this

    override fun getZone(): ZoneId = ZoneId.systemDefault()

    override fun instant(): Instant = currentTime
}