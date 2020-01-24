package org.http4k.security.oauth.server

import com.natpryce.Failure
import com.natpryce.Result
import com.natpryce.Success
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.apache.commons.codec.binary.Base64
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.security.ResponseType.Code
import org.http4k.security.State
import org.http4k.security.oauth.server.request.RequestJWTValidator
import org.http4k.security.oauth.server.request.RequestObject
import org.http4k.security.oauth.server.request.RequestObjectExtractor.RequestObjectExtractorJson
import org.http4k.security.openid.RequestJwtContainer
import org.junit.jupiter.api.Test

internal class AuthRequestWithRequestAuthRequestExtractorTest {

    private val invalidJwt = RequestJwtContainer("invalidJwt")

    private val requestJWTValidator = object : RequestJWTValidator {
        override fun validate(clientId: ClientId, requestJwtContainer: RequestJwtContainer): InvalidAuthorizationRequest? {
            return if (requestJwtContainer == invalidJwt) {
                InvalidAuthorizationRequest("Query 'request' is invalid")
            } else {
                null
            }
        }
    }

    private val underTest = AuthRequestWithRequestAuthRequestExtractor(requestJWTValidator)

    @Test
    fun `if no request jwt then do nothing, just treat it as a normal request`() {
        assertThat(underTest.extract(Request(GET, "/?client_id=12345&response_type=code&redirect_uri=https://somehost")), equalTo(success(AuthRequest(
            client = ClientId("12345"),
            responseType = Code,
            redirectUri = Uri.of("https://somehost"),
            scopes = emptyList(),
            state = null
        ))))
    }

    @Test
    fun `if has request jwt but not valid then error`() {
        assertThat(underTest.extract(Request(GET, "/?client_id=12345&response_type=code&redirect_uri=https://somehost&request=invalidJwt")),
            equalTo(failure(InvalidAuthorizationRequest("Query 'request' is invalid"))))
    }

    @Test
    fun `if has 'valid' request jwt but fails parsing then error`() {
        assertThat(underTest.extract(Request(GET, "/?client_id=12345&response_type=code&redirect_uri=https://somehost&request=someInvalidButCorrectlySigned")),
            equalTo(failure(InvalidAuthorizationRequest("Query 'request' is invalid"))))
    }

    @Test
    fun `if no client_id on request uri but in request jwt then error`() {
        val requestObject = RequestObject(client = ClientId("12345"))
        val requestObjectJwt = requestJwt(requestObject)
        assertThat(underTest.extract(Request(GET, "/?response_type=code&redirect_uri=https://somehost&request=$requestObjectJwt")), equalTo(failure(InvalidAuthorizationRequest("query 'client_id' is required"))))
    }

    @Test
    fun `if client_id on request uri but has different on in request jwt then error`() {
        val requestObject = RequestObject(client = ClientId("54321"))
        val requestObjectJwt = requestJwt(requestObject)
        assertThat(underTest.extract(Request(GET, "/?client_id=12345&response_type=code&redirect_uri=https://somehost&request=$requestObjectJwt")), equalTo(failure(InvalidAuthorizationRequest("'client_id' is invalid"))))
    }

    @Test
    fun `if client_id on request uri and has same one in request jwt then success`() {
        val requestObject = RequestObject(client = ClientId("12345"))
        val requestObjectJwt = requestJwt(requestObject)
        assertThat(underTest.extract(Request(GET, "/?client_id=12345&response_type=code&redirect_uri=https://somehost&request=$requestObjectJwt")), equalTo(success(AuthRequest(
            client = ClientId("12345"),
            responseType = Code,
            redirectUri = Uri.of("https://somehost"),
            scopes = emptyList(),
            state = null,
            request = RequestJwtContainer(requestObjectJwt),
            requestObject = requestObject
        ))))
    }

    @Test
    fun `if redirect_uri is null on request but available on request object user that one`() {
        val requestObject = RequestObject(redirectUri = Uri.of("https://somehost"))
        val requestObjectJwt = requestJwt(requestObject)
        assertThat(underTest.extract(Request(GET, "/?client_id=12345&response_type=code&request=$requestObjectJwt")), equalTo(success(AuthRequest(
            client = ClientId("12345"),
            responseType = Code,
            redirectUri = Uri.of("https://somehost"),
            scopes = emptyList(),
            state = null,
            requestObject = requestObject,
            request = RequestJwtContainer(requestObjectJwt)
        ))))
    }

    @Test
    fun `scopes are the same on request and request object but in different order than it is correct`() {
        val requestObject = RequestObject(scope = "email openid address")
        val requestObjectJwt = requestJwt(requestObject)
        assertThat(underTest.extract(Request(GET, "/?client_id=12345&scope=openid+email+address=&response_type=code&redirect_uri=https://somehost&request=$requestObjectJwt")), equalTo(success(AuthRequest(
            client = ClientId("12345"),
            responseType = Code,
            redirectUri = Uri.of("https://somehost"),
            scopes = listOf("openid", "email", "address"),
            state = null,
            requestObject = requestObject,
            request = RequestJwtContainer(requestObjectJwt)
        ))))
    }

    @Test
    fun `if scopes on the request are missing but available on the request jwt`() {
        val requestObject = RequestObject(scope = "email openid address")
        val requestObjectJwt = requestJwt(requestObject)
        assertThat(underTest.extract(Request(GET, "/?client_id=12345=&response_type=code&redirect_uri=https://somehost&request=$requestObjectJwt")), equalTo(success(AuthRequest(
            client = ClientId("12345"),
            responseType = Code,
            redirectUri = Uri.of("https://somehost"),
            scopes = listOf("email", "openid", "address"),
            state = null,
            requestObject = requestObject,
            request = RequestJwtContainer(requestObjectJwt)
        ))))
    }

    @Test
    fun `if scopes on the request are missing but missing on the request jwt`() {
        val requestObject = RequestObject(state = State("some state"))
        val requestObjectJwt = requestJwt(requestObject)
        assertThat(underTest.extract(Request(GET, "/?client_id=12345=&response_type=code&scope=openid+email+address&redirect_uri=https://somehost&request=$requestObjectJwt")), equalTo(success(AuthRequest(
            client = ClientId("12345"),
            responseType = Code,
            redirectUri = Uri.of("https://somehost"),
            scopes = listOf("openid", "email", "address"),
            state = State("some state"),
            requestObject = requestObject,
            request = RequestJwtContainer(requestObjectJwt)
        ))))
    }

    private fun success(authRequest: AuthRequest): Result<AuthRequest, InvalidAuthorizationRequest> = Success(authRequest)
    private fun failure(error: InvalidAuthorizationRequest): Result<AuthRequest, InvalidAuthorizationRequest> = Failure(error)

    private fun requestJwt(requestObject: RequestObject): String =
        "someHeader.${Base64.encodeBase64URLSafeString(RequestObjectExtractorJson.asJsonString(requestObject).toByteArray()).replace("=", "")}.someSignature"


}
