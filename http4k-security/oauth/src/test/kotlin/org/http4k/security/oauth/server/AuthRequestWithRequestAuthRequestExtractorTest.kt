package org.http4k.security.oauth.server

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory.instance
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.format.ConfigurableJackson
import org.http4k.format.asConfigurable
import org.http4k.format.text
import org.http4k.security.Nonce
import org.http4k.security.ResponseMode
import org.http4k.security.ResponseType
import org.http4k.security.ResponseType.Code
import org.http4k.security.State
import org.http4k.security.oauth.server.AuthRequestWithRequestAuthRequestExtractor.CombineAuthRequestRequestStrategy.AuthRequestOnly
import org.http4k.security.oauth.server.AuthRequestWithRequestAuthRequestExtractor.CombineAuthRequestRequestStrategy.Combine
import org.http4k.security.oauth.server.AuthRequestWithRequestAuthRequestExtractor.CombineAuthRequestRequestStrategy.RequestObjectOnly
import org.http4k.security.oauth.server.request.Claims
import org.http4k.security.oauth.server.request.RequestJWTValidator
import org.http4k.security.oauth.server.request.RequestObject
import org.http4k.security.openid.RequestJwtContainer
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class AuthRequestWithRequestAuthRequestExtractorTest {

    private val invalidJwt = RequestJwtContainer("invalidJwt")

    private val requestJWTValidator = RequestJWTValidator { _, requestJwtContainer ->
        if (requestJwtContainer == invalidJwt) InvalidAuthorizationRequest("Query 'request' is invalid") else null
    }

    @Test
    fun `if no request jwt then do nothing, just treat it as a normal request`() {
        assertThat(
            underTest().extract(Request(GET, "/?client_id=12345&response_type=code&redirect_uri=https://somehost")),
            equalTo(
                success(
                    AuthRequest(
                        client = ClientId("12345"),
                        responseType = Code,
                        redirectUri = Uri.of("https://somehost"),
                        scopes = emptyList(),
                        state = null
                    )
                )
            )
        )
    }

    @Test
    fun `if has request jwt but not valid then error`() {
        assertThat(
            underTest().extract(
                Request(
                    GET,
                    "/?client_id=12345&response_type=code&redirect_uri=https://somehost&request=invalidJwt"
                )
            ),
            equalTo(failure(InvalidAuthorizationRequest("Query 'request' is invalid")))
        )
    }

    @Test
    fun `if has 'valid' request jwt but fails parsing then error`() {
        assertThat(
            underTest().extract(
                Request(
                    GET,
                    "/?client_id=12345&response_type=code&redirect_uri=https://somehost&request=someInvalidButCorrectlySigned"
                )
            ),
            equalTo(failure(InvalidAuthorizationRequest("Query 'request' is invalid")))
        )
    }

    @Test
    fun `if no client_id on request uri but in request jwt then error`() {
        val requestObject = RequestObject(client = ClientId("12345"))
        val requestObjectJwt = requestJwt(requestObject)
        assertThat(
            underTest().extract(
                Request(
                    GET,
                    "/?response_type=code&redirect_uri=https://somehost&request=$requestObjectJwt"
                )
            ), equalTo(failure(InvalidAuthorizationRequest("query 'client_id' is required")))
        )
    }

    @Test
    fun `if client_id on request uri but has different on in request jwt then error`() {
        val requestObject = RequestObject(client = ClientId("54321"))
        val requestObjectJwt = requestJwt(requestObject)
        assertThat(
            underTest().extract(
                Request(
                    GET,
                    "/?client_id=12345&response_type=code&redirect_uri=https://somehost&request=$requestObjectJwt"
                )
            ), equalTo(failure(InvalidAuthorizationRequest("'client_id' is invalid")))
        )
    }

    @Nested
    @DisplayName("Combine AuthRequest and RequestObject")
    inner class CombineAuthRequestAndRequestObject {

        @Test
        fun `if client_id on request uri and has same one in request jwt then success`() {
            val requestObject = RequestObject(client = ClientId("12345"))
            val requestObjectJwt = requestJwt(requestObject)
            assertThat(
                underTest().extract(
                    Request(
                        GET,
                        "/?client_id=12345&response_type=code&redirect_uri=https://somehost&request=$requestObjectJwt"
                    )
                ), equalTo(
                    success(
                        AuthRequest(
                            client = ClientId("12345"),
                            responseType = Code,
                            redirectUri = Uri.of("https://somehost"),
                            scopes = emptyList(),
                            state = null,
                            request = RequestJwtContainer(requestObjectJwt),
                            requestObject = requestObject
                        )
                    )
                )
            )
        }

        @Test
        fun `if redirect_uri is null on request but available on request object user that one`() {
            val requestObject = RequestObject(redirectUri = Uri.of("https://somehost"))
            val requestObjectJwt = requestJwt(requestObject)
            assertThat(
                underTest().extract(Request(GET, "/?client_id=12345&response_type=code&request=$requestObjectJwt")),
                equalTo(
                    success(
                        AuthRequest(
                            client = ClientId("12345"),
                            responseType = Code,
                            redirectUri = Uri.of("https://somehost"),
                            scopes = emptyList(),
                            state = null,
                            requestObject = requestObject,
                            request = RequestJwtContainer(requestObjectJwt)
                        )
                    )
                )
            )
        }

        @Test
        fun `scopes are the same on request and request object but in different order than it is correct`() {
            val requestObject = RequestObject(scope = listOf("email", "openid", "address"))
            val requestObjectJwt = requestJwt(requestObject)
            assertThat(
                underTest().extract(
                    Request(
                        GET,
                        "/?client_id=12345&scope=openid+email+address=&response_type=code&redirect_uri=https://somehost&request=$requestObjectJwt"
                    )
                ), equalTo(
                    success(
                        AuthRequest(
                            client = ClientId("12345"),
                            responseType = Code,
                            redirectUri = Uri.of("https://somehost"),
                            scopes = listOf("openid", "email", "address"),
                            state = null,
                            requestObject = requestObject,
                            request = RequestJwtContainer(requestObjectJwt)
                        )
                    )
                )
            )
        }

        @Test
        fun `if scopes on the request are missing but available on the request jwt`() {
            val requestObject = RequestObject(scope = listOf("email", "openid", "address"))
            val requestObjectJwt = requestJwt(requestObject)
            assertThat(
                underTest().extract(
                    Request(
                        GET,
                        "/?client_id=12345=&response_type=code&redirect_uri=https://somehost&request=$requestObjectJwt"
                    )
                ), equalTo(
                    success(
                        AuthRequest(
                            client = ClientId("12345"),
                            responseType = Code,
                            redirectUri = Uri.of("https://somehost"),
                            scopes = listOf("email", "openid", "address"),
                            state = null,
                            requestObject = requestObject,
                            request = RequestJwtContainer(requestObjectJwt)
                        )
                    )
                )
            )
        }

        @Test
        fun `if scopes on the request are missing but missing on the request jwt`() {
            val requestObject = RequestObject(state = State("some state"))
            val requestObjectJwt = requestJwt(requestObject)
            assertThat(
                underTest().extract(
                    Request(
                        GET,
                        "/?client_id=12345=&response_type=code&scope=openid+email+address&redirect_uri=https://somehost&request=$requestObjectJwt"
                    )
                ), equalTo(
                    success(
                        AuthRequest(
                            client = ClientId("12345"),
                            responseType = Code,
                            redirectUri = Uri.of("https://somehost"),
                            scopes = listOf("openid", "email", "address"),
                            state = State("some state"),
                            requestObject = requestObject,
                            request = RequestJwtContainer(requestObjectJwt)
                        )
                    )
                )
            )
        }
    }

    @Nested
    @DisplayName("UseAuthRequest")
    inner class AuthRequestOnly {

        @Test
        fun `if client_id on request uri and has same one in request jwt then success`() {
            val requestObject = RequestObject(client = ClientId("12345"))
            val requestObjectJwt = requestJwt(requestObject)
            assertThat(
                underTest(AuthRequestOnly).extract(
                    Request(
                        GET,
                        "/?client_id=12345&response_type=code&redirect_uri=https://somehost&request=$requestObjectJwt"
                    )
                ), equalTo(
                    success(
                        AuthRequest(
                            client = ClientId("12345"),
                            responseType = Code,
                            redirectUri = Uri.of("https://somehost"),
                            scopes = emptyList(),
                            state = null,
                            request = RequestJwtContainer(requestObjectJwt),
                            requestObject = requestObject
                        )
                    )
                )
            )
        }

        @Test
        fun `if redirect_uri is null on request but available on request object user that one`() {
            val requestObject = RequestObject(redirectUri = Uri.of("https://somehost"))
            val requestObjectJwt = requestJwt(requestObject)
            assertThat(
                underTest(AuthRequestOnly).extract(
                    Request(
                        GET,
                        "/?client_id=12345&response_type=code&request=$requestObjectJwt"
                    )
                ), equalTo(
                    success(
                        AuthRequest(
                            client = ClientId("12345"),
                            responseType = Code,
                            redirectUri = null,
                            scopes = emptyList(),
                            state = null,
                            requestObject = requestObject,
                            request = RequestJwtContainer(requestObjectJwt)
                        )
                    )
                )
            )
        }

        @Test
        fun `scopes are the same on request and request object but in different order than it is correct`() {
            val requestObject = RequestObject(scope = listOf("email", "openid", "address"))
            val requestObjectJwt = requestJwt(requestObject)
            assertThat(
                underTest(AuthRequestOnly).extract(
                    Request(
                        GET,
                        "/?client_id=12345&scope=openid+email+address=&response_type=code&redirect_uri=https://somehost&request=$requestObjectJwt"
                    )
                ), equalTo(
                    success(
                        AuthRequest(
                            client = ClientId("12345"),
                            responseType = Code,
                            redirectUri = Uri.of("https://somehost"),
                            scopes = listOf("openid", "email", "address"),
                            state = null,
                            requestObject = requestObject,
                            request = RequestJwtContainer(requestObjectJwt)
                        )
                    )
                )
            )
        }

        @Test
        fun `if scopes on the request are missing but available on the request jwt, only use the auth request one`() {
            val requestObject = RequestObject(scope = listOf("email", "openid", "address"))
            val requestObjectJwt = requestJwt(requestObject)
            assertThat(
                underTest(AuthRequestOnly).extract(
                    Request(
                        GET,
                        "/?client_id=12345=&response_type=code&redirect_uri=https://somehost&request=$requestObjectJwt"
                    )
                ), equalTo(
                    success(
                        AuthRequest(
                            client = ClientId("12345"),
                            responseType = Code,
                            redirectUri = Uri.of("https://somehost"),
                            scopes = emptyList(),
                            state = null,
                            requestObject = requestObject,
                            request = RequestJwtContainer(requestObjectJwt)
                        )
                    )
                )
            )
        }

        @Test
        fun `if scopes on the request are missing but missing on the request jwt, only use the auth request one`() {
            val requestObject = RequestObject(state = State("some state"))
            val requestObjectJwt = requestJwt(requestObject)
            assertThat(
                underTest(AuthRequestOnly).extract(
                    Request(
                        GET,
                        "/?client_id=12345=&response_type=code&scope=openid+email+address&redirect_uri=https://somehost&request=$requestObjectJwt"
                    )
                ), equalTo(
                    success(
                        AuthRequest(
                            client = ClientId("12345"),
                            responseType = Code,
                            redirectUri = Uri.of("https://somehost"),
                            scopes = listOf("openid", "email", "address"),
                            state = null,
                            requestObject = requestObject,
                            request = RequestJwtContainer(requestObjectJwt)
                        )
                    )
                )
            )
        }
    }

    @Nested
    @DisplayName("Use only request object")
    inner class RequestObjectOnly {

        @Test
        fun `if client_id on request uri and has same one in request jwt then success`() {
            val requestObject = RequestObject(client = ClientId("12345"))
            val requestObjectJwt = requestJwt(requestObject)
            assertThat(
                underTest(RequestObjectOnly).extract(
                    Request(
                        GET,
                        "/?client_id=12345&response_type=code&redirect_uri=https://somehost&request=$requestObjectJwt"
                    )
                ), equalTo(
                    success(
                        AuthRequest(
                            client = ClientId("12345"),
                            responseType = Code,
                            redirectUri = requestObject.redirectUri,
                            scopes = requestObject.scope,
                            state = requestObject.state,
                            request = RequestJwtContainer(requestObjectJwt),
                            requestObject = requestObject
                        )
                    )
                )
            )
        }

        @Test
        fun `if redirect_uri is null on request but available on request object user that one`() {
            val requestObject = RequestObject(redirectUri = Uri.of("https://somehost"))
            val requestObjectJwt = requestJwt(requestObject)
            assertThat(
                underTest(RequestObjectOnly).extract(
                    Request(
                        GET,
                        "/?client_id=12345&response_type=code&request=$requestObjectJwt"
                    )
                ), equalTo(
                    success(
                        AuthRequest(
                            client = ClientId("12345"),
                            responseType = Code,
                            redirectUri = requestObject.redirectUri,
                            scopes = requestObject.scope,
                            state = requestObject.state,
                            requestObject = requestObject,
                            request = RequestJwtContainer(requestObjectJwt)
                        )
                    )
                )
            )
        }

        @Test
        fun `scopes are the same on request and request object but in different order than it is correct`() {
            val requestObject = RequestObject(scope = listOf("email", "openid", "address"))
            val requestObjectJwt = requestJwt(requestObject)
            assertThat(
                underTest(RequestObjectOnly).extract(
                    Request(
                        GET,
                        "/?client_id=12345&scope=openid+email+address=&response_type=code&redirect_uri=https://somehost&request=$requestObjectJwt"
                    )
                ), equalTo(
                    success(
                        AuthRequest(
                            client = ClientId("12345"),
                            responseType = Code,
                            redirectUri = requestObject.redirectUri,
                            scopes = requestObject.scope,
                            state = requestObject.state,
                            requestObject = requestObject,
                            request = RequestJwtContainer(requestObjectJwt)
                        )
                    )
                )
            )
        }

        @Test
        fun `if scopes on the request are missing but available on the request jwt, only use the request object one`() {
            val requestObject = RequestObject(scope = listOf("email", "openid", "address"))
            val requestObjectJwt = requestJwt(requestObject)
            assertThat(
                underTest(RequestObjectOnly).extract(
                    Request(
                        GET,
                        "/?client_id=12345=&response_type=code&redirect_uri=https://somehost&request=$requestObjectJwt"
                    )
                ), equalTo(
                    success(
                        AuthRequest(
                            client = ClientId("12345"),
                            responseType = Code,
                            redirectUri = requestObject.redirectUri,
                            scopes = requestObject.scope,
                            state = requestObject.state,
                            requestObject = requestObject,
                            request = RequestJwtContainer(requestObjectJwt)
                        )
                    )
                )
            )
        }

        @Test
        fun `if scopes on the request are missing but missing on the request jwt, only use the request object one`() {
            val requestObject = RequestObject(state = State("some state"))
            val requestObjectJwt = requestJwt(requestObject)
            assertThat(
                underTest(RequestObjectOnly).extract(
                    Request(
                        GET,
                        "/?client_id=12345=&response_type=code&scope=openid+email+address&redirect_uri=https://somehost&request=$requestObjectJwt"
                    )
                ), equalTo(
                    success(
                        AuthRequest(
                            client = ClientId("12345"),
                            responseType = Code,
                            redirectUri = requestObject.redirectUri,
                            scopes = requestObject.scope,
                            state = requestObject.state,
                            requestObject = requestObject,
                            request = RequestJwtContainer(requestObjectJwt)
                        )
                    )
                )
            )
        }
    }

    private fun success(authRequest: AuthRequest): Result<AuthRequest, InvalidAuthorizationRequest> =
        Success(authRequest)

    private fun failure(error: InvalidAuthorizationRequest): Result<AuthRequest, InvalidAuthorizationRequest> =
        Failure(error)

    private fun requestJwt(requestObject: RequestObject): String {
        val requestObjectJson = RequestObjectJson(
            client = requestObject.client,
            redirectUri = requestObject.redirectUri,
            audience = audienceToJson(requestObject.audience),
            issuer = requestObject.issuer,
            scope = if (requestObject.scope.isEmpty()) null else requestObject.scope.joinToString(" "),
            responseMode = requestObject.responseMode,
            responseType = requestObject.responseType,
            state = requestObject.state,
            nonce = requestObject.nonce,
            magAge = requestObject.magAge,
            expiry = requestObject.expiry,
            claims = requestObject.claims
        )
        return "someHeader.${
            encodeBase64URLSafeString(
                RequestObjectExtractorJson.asFormatString(requestObjectJson).toByteArray()
            ).replace("=", "")
        }.someSignature"
    }

    private fun audienceToJson(audience: List<String>) = when {
        audience.isEmpty() -> NullNode.instance
        audience.size == 1 -> TextNode(audience[0])
        else -> ArrayNode(instance, audience.map { TextNode(it) })
    }

    private fun underTest(strategy: AuthRequestWithRequestAuthRequestExtractor.CombineAuthRequestRequestStrategy = Combine) =
        AuthRequestWithRequestAuthRequestExtractor(requestJWTValidator, strategy)
}

internal data class RequestObjectJson(
    @JsonProperty("client_id") val client: ClientId? = null,
    @JsonProperty("redirect_uri") val redirectUri: Uri? = null,
    @JsonProperty("aud") val audience: JsonNode,
    @JsonProperty("iss") val issuer: String? = null,
    @JsonProperty("scope") val scope: String?,
    @JsonProperty("response_mode") val responseMode: ResponseMode? = null,
    @JsonProperty("response_type") val responseType: ResponseType? = null,
    @JsonProperty("state") val state: State? = null,
    @JsonProperty("nonce") val nonce: Nonce? = null,
    @JsonProperty("max_age") val magAge: Long? = null,
    @JsonProperty("exp") val expiry: Long? = null,
    @JsonProperty("claims") val claims: Claims = Claims()
)

internal object RequestObjectExtractorJson : ConfigurableJackson(
    KotlinModule()
        .asConfigurable()
        .text(Uri.Companion::of, Uri::toString)
        .text(::ClientId, ClientId::value)
        .text(::State, State::value)
        .text(::Nonce, Nonce::value)
        .text(ResponseMode.Companion::fromQueryParameterValue, ResponseMode::queryParameterValue)
        .text(ResponseType.Companion::fromQueryParameterValue, ResponseType::queryParameterValue)
        .done()
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
        .configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true)
        .configure(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS, true)
        .configure(DeserializationFeature.FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY, false)
)
