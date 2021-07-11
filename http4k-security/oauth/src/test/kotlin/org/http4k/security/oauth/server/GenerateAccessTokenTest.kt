package org.http4k.security.oauth.server

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.get
import org.http4k.core.ContentType
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.Uri
import org.http4k.core.body.form
import org.http4k.format.AutoMarshallingJson
import org.http4k.format.Jackson
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.http4k.security.AccessTokenResponse
import org.http4k.security.ResponseType.CodeIdToken
import org.http4k.security.State
import org.http4k.security.accessTokenResponseBody
import org.http4k.security.oauth.server.accesstoken.ClientSecretAccessTokenRequestAuthentication
import org.http4k.security.oauth.server.accesstoken.GrantType
import org.http4k.security.oauth.server.accesstoken.GrantTypesConfiguration
import org.http4k.util.FixedClock
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit.SECONDS
import java.time.temporal.TemporalUnit

class GenerateAccessTokenTest {

    private val json: AutoMarshallingJson<*> = Jackson
    private val handlerClock = SettableClock()
    private val codes = InMemoryAuthorizationCodes(FixedClock)
    private val authRequest = AuthRequest(ClientId("a-clientId"), listOf(), Uri.of("redirect"), State("state"))
    private val request = Request(GET, "http://some-thing")
    private val code = codes.create(request, authRequest, Response(OK)).get()
    private val clientValidator = HardcodedClientValidator(authRequest.client, authRequest.redirectUri!!, "a-secret")
    private val handler = GenerateAccessToken(codes, DummyAccessTokens(), handlerClock, DummyIdTokens(), DummyRefreshTokens(), JsonResponseErrorRenderer(json), GrantTypesConfiguration.default(ClientSecretAccessTokenRequestAuthentication(clientValidator)))

    @Test
    fun `generates a dummy token`() {
        val response = handler(Request(POST, "/token")
            .header("content-type", ContentType.APPLICATION_FORM_URLENCODED.value)
            .form("grant_type", "authorization_code")
            .form("code", code.value)
            .form("client_id", authRequest.client.value)
            .form("client_secret", "a-secret")
            .form("redirect_uri", authRequest.redirectUri.toString())
        )

        assertThat(response, hasStatus(OK) and hasBody(accessTokenResponseBody, equalTo(AccessTokenResponse("dummy-access-token", "Bearer"))))
    }

    @Test
    fun `generates dummy access_token and id_token if an oidc request`() {
        val codeForIdTokenRequest = codes.create(request, authRequest.copy(scopes = listOf("openid")), Response(OK)).get()

        val response = handler(Request(POST, "/token")
            .header("content-type", ContentType.APPLICATION_FORM_URLENCODED.value)
            .form("grant_type", "authorization_code")
            .form("code", codeForIdTokenRequest.value)
            .form("client_id", authRequest.client.value)
            .form("client_secret", "a-secret")
            .form("redirect_uri", authRequest.redirectUri.toString())
        )

        assertThat(response, hasStatus(OK))

        assertThat(accessTokenResponseBody(response), equalTo(AccessTokenResponse("dummy-access-token", "Bearer", id_token = "dummy-id-token-for-access-token")))
    }

    @Test
    fun `allowing refreshing a token`() {
        val response = handler(Request(POST, "/token")
            .header("content-type", ContentType.APPLICATION_FORM_URLENCODED.value)
            .form("grant_type", "refresh_token")
            .form("refresh_token", "valid-refresh-token")
            .form("client_id", authRequest.client.value)
            .form("client_secret", "a-secret"))

        assertThat(response, hasStatus(OK) and hasBody(accessTokenResponseBody, equalTo(AccessTokenResponse(
            access_token = DummyRefreshTokens.newAccessToken.value,
            token_type = DummyRefreshTokens.newAccessToken.type,
            scope = DummyRefreshTokens.newAccessToken.scope,
            expires_in = DummyRefreshTokens.newAccessToken.expiresIn,
            refresh_token = DummyRefreshTokens.newAccessToken.refreshToken?.value
        ))))
    }

    @Test
    fun `bad request for missing refresh_token parameter`() {
        val response = handler(Request(POST, "/token")
            .header("content-type", ContentType.APPLICATION_FORM_URLENCODED.value)
            .form("grant_type", "refresh_token")
            .form("client_id", authRequest.client.value)
            .form("client_secret", "a-secret"))

        assertThat(response, hasStatus(BAD_REQUEST) and hasBody(withErrorType("invalid_request")))
    }

    @Test
    fun `validates credentials for refresh tokens`() {
        val response = handler(Request(POST, "/token")
            .header("content-type", ContentType.APPLICATION_FORM_URLENCODED.value)
            .form("grant_type", "refresh_token")
            .form("refresh_token", "invalid-refresh-token")
            .form("client_id", authRequest.client.value)
            .form("client_secret", "not a valid secret"))

        assertThat(response, hasStatus(UNAUTHORIZED) and hasBody(withErrorType("invalid_client")))
    }

    @Test
    fun `copes with error from actual refresh tokens`() {
        val response = handler(Request(POST, "/token")
            .header("content-type", ContentType.APPLICATION_FORM_URLENCODED.value)
            .form("grant_type", "refresh_token")
            .form("refresh_token", "invalid-refresh-token")
            .form("client_id", authRequest.client.value)
            .form("client_secret", "a-secret"))

        assertThat(response, hasStatus(BAD_REQUEST) and hasBody(withErrorType("invalid_request")))
    }

    @Test
    fun `generates dummy access_token and id_token`() {
        val codeForIdTokenRequest = codes.create(request, authRequest.copy(responseType = CodeIdToken, scopes = listOf("openid")), Response(OK)).get()

        val response = handler(Request(POST, "/token")
            .header("content-type", ContentType.APPLICATION_FORM_URLENCODED.value)
            .form("grant_type", "authorization_code")
            .form("code", codeForIdTokenRequest.value)
            .form("client_id", authRequest.client.value)
            .form("client_secret", "a-secret")
            .form("redirect_uri", authRequest.redirectUri.toString())
        )

        assertThat(response, hasStatus(OK))

        assertThat(accessTokenResponseBody(response), equalTo(AccessTokenResponse("dummy-access-token", "Bearer", id_token = "dummy-id-token-for-access-token")))
    }

    @Test
    fun `generates dummy token for client credentials grant type`() {
        val response = handler(Request(POST, "/token")
            .header("content-type", ContentType.APPLICATION_FORM_URLENCODED.value)
            .form("grant_type", "client_credentials")
            .form("client_secret", "a-secret")
            .form("client_id", authRequest.client.value)
        )

        assertThat(response, hasStatus(OK) and hasBody(accessTokenResponseBody, equalTo(AccessTokenResponse("dummy-access-token", "Bearer"))))
    }

    @Test
    fun `handles invalid grant_type`() {
        val response = handler(Request(POST, "/token")
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
        val response = handler(Request(POST, "/token")
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

        val expiredCode = codes.create(request, authRequest, Response(OK)).get()

        val response = handler(Request(POST, "/token")
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
    fun `handles client id different from one in authorization code`() {
        val storedCode = codes.create(request, authRequest.copy(client = ClientId("different client")), Response(OK)).get()

        val response = handler(Request(POST, "/token")
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
    fun `handles redirectUri different from one in authorization code`() {
        val storedCode = codes.create(request, authRequest.copy(redirectUri = Uri.of("somethingelse")), Response(OK)).get()

        val response = handler(Request(POST, "/token")
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
    fun `handles already used authentication code`() {
        val handler = GenerateAccessToken(codes, ErroringAccessTokens(AuthorizationCodeAlreadyUsed), handlerClock, DummyIdTokens(), DummyRefreshTokens(), JsonResponseErrorRenderer(json), GrantTypesConfiguration.default(ClientSecretAccessTokenRequestAuthentication(clientValidator)))
        val request = Request(POST, "/token")
            .header("content-type", ContentType.APPLICATION_FORM_URLENCODED.value)
            .form("grant_type", "authorization_code")
            .form("code", code.value)
            .form("client_id", authRequest.client.value)
            .form("client_secret", "a-secret")
            .form("redirect_uri", authRequest.redirectUri.toString())
        val response = handler(request)

        assertThat(response, hasStatus(BAD_REQUEST) and hasBody(withErrorType("invalid_grant")))
    }

    @Test
    fun `correctly returns documentation uri if provided`() {
        val documentationUri = "SomeUri"
        val handler = GenerateAccessToken(codes, ErroringAccessTokens(AuthorizationCodeAlreadyUsed), handlerClock, DummyIdTokens(), DummyRefreshTokens(), JsonResponseErrorRenderer(json, documentationUri), GrantTypesConfiguration.default(ClientSecretAccessTokenRequestAuthentication(clientValidator)))
        val request = Request(POST, "/token")
            .header("content-type", ContentType.APPLICATION_FORM_URLENCODED.value)
            .form("grant_type", "authorization_code")
            .form("code", code.value)
            .form("client_id", authRequest.client.value)
            .form("client_secret", "a-secret")
            .form("redirect_uri", authRequest.redirectUri.toString())
        val response = handler(request)

        assertThat(response, hasStatus(BAD_REQUEST) and hasBody(withErrorTypeAndUri("invalid_grant", documentationUri)))
    }

    @Test
    fun `handles grant type not in configuration`() {
        val handler = GenerateAccessToken(codes, ErroringAccessTokens(AuthorizationCodeAlreadyUsed), handlerClock, DummyIdTokens(), DummyRefreshTokens(), JsonResponseErrorRenderer(json), GrantTypesConfiguration(emptyMap()))

        val response = handler(Request(POST, "/token")
            .header("content-type", ContentType.APPLICATION_FORM_URLENCODED.value)
            .form("grant_type", GrantType.ClientCredentials.rfcValue)
            .form("client_id", authRequest.client.value)
            .form("client_secret", "a-secret")
        )

        assertThat(response, hasStatus(BAD_REQUEST) and hasBody(withErrorType("unsupported_grant_type")))
    }

    private fun withErrorType(errorType: String) =
        containsSubstring("\"error\":\"$errorType\"")
            .and(containsSubstring("\"error_description\":"))
            .and(containsSubstring("\"error_uri\":null"))

    private fun withErrorTypeAndUri(errorType: String, errorUri: String) = containsSubstring("\"error\":\"$errorType\"")
        .and(containsSubstring("\"error_description\":"))
        .and(containsSubstring("\"error_uri\":\"${errorUri}\""))
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
