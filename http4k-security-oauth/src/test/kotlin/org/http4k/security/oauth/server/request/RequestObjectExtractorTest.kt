package org.http4k.security.oauth.server.request

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.apache.commons.codec.binary.Base64
import org.http4k.core.Uri
import org.http4k.format.Jackson
import org.http4k.security.ResponseMode.Query
import org.http4k.security.ResponseType.CodeIdToken
import org.http4k.security.State
import org.http4k.security.oauth.server.ClientId
import org.http4k.security.oauth.server.InvalidRequestObject
import org.http4k.security.oauth.server.request.RequestObjectExtractor.extractRequestJwtClaimsAsMap
import org.http4k.security.openid.Nonce
import org.junit.jupiter.api.Test
import java.time.Instant

internal class RequestObjectExtractorTest {

    @Test
    fun `if not three parts then failure`() {
        assertThat(extractRequestJwtClaimsAsMap("kasdjflksadjfsjdfaksjdf"), equalTo(failure()))
        assertThat(extractRequestJwtClaimsAsMap("kasdjflksadj.fsjdfaksjdf"), equalTo(failure()))
        assertThat(extractRequestJwtClaimsAsMap("kasdjfl.ksadj.fsjdfa.ksjdf"), equalTo(failure()))
    }

    @Test
    fun `if has three parts but middle part is not valid base64 encoded`() {
        assertThat(extractRequestJwtClaimsAsMap("kasdjfl.ksadjfsjd.faksjdf"), equalTo(failure()))
    }

    @Test
    fun `if middle part is correctly base64 encoded but not json then error`() {
        assertThat(
            extractRequestJwtClaimsAsMap("kasdjfl.${Base64.encodeBase64String("something not json".toByteArray())}.faksjdf"),
            equalTo(failure())
        )
    }

    @Test
    fun `if middle part is correctly base64 encoded json then success`() {
        assertThat(
            extractRequestJwtClaimsAsMap("kasdjfl.${Base64.encodeBase64String("{\"foo\":\"bar\"}".toByteArray())}.faksjdf"),
            equalTo(success(mapOf("foo" to "bar") as Map<*, *>))
        )
    }

    @Test
    fun `parses 'full' object correctly`() {
        val expiry = Instant.now().epochSecond
        val rawData = mapOf(
            "iss" to "s6BhdRkqt3",
            "aud" to "https://server.example.com",
            "response_mode" to "query",
            "response_type" to "code id_token",
            "client_id" to "s6BhdRkqt3",
            "redirect_uri" to "https://client.example.org/cb",
            "scope" to "openid",
            "state" to "af0ifjsldkj",
            "nonce" to "n-0S6_WzA2Mj",
            "max_age" to 86400,
            "exp" to expiry,
            "claims" to mapOf(
                "userinfo" to mapOf(
                    "given_name" to mapOf("essential" to true),
                    "email" to mapOf("essential" to true),
                    "email_verified" to mapOf("essential" to true),
                    "someThingCustomer" to mapOf("value" to "someCustomerValue", "essential" to true)
                ),
                "id_token" to mapOf(
                    "birthdate" to mapOf("essential" to true),
                    "acr" to mapOf("values" to listOf("urn:mace:incommon:iap:silver"))
                )
            )
        )

        val correspondingExpectedRequestObject = RequestObject(
            issuer = "s6BhdRkqt3",
            audience = listOf("https://server.example.com"),
            responseMode = Query,
            responseType = CodeIdToken,
            client = ClientId("s6BhdRkqt3"),
            redirectUri = Uri.of("https://client.example.org/cb"),
            scope = listOf("openid"),
            state = State("af0ifjsldkj"),
            nonce = Nonce("n-0S6_WzA2Mj"),
            magAge = 86400,
            expiry = expiry,
            claims = Claims(
                userInfo = mapOf(
                    "given_name" to Claim(true),
                    "email" to Claim(true),
                    "email_verified" to Claim(true),
                    "someThingCustomer" to Claim(true, value = "someCustomerValue")
                ),
                idToken = mapOf(
                    "birthdate" to Claim(true),
                    "acr" to Claim(false, values = listOf("urn:mace:incommon:iap:silver"))
                )
            )
        )

        val requestJwt = "someHeader.${Base64.encodeBase64URLSafeString(Jackson.asFormatString(rawData).toByteArray()).replace("=", "")}.someSignature"

        assertThat(RequestObjectExtractor.extractRequestObjectFromJwt(requestJwt), equalTo(success(correspondingExpectedRequestObject)))
    }

    @Test
    fun `if has unknown fields ignore them, and parse known ones`() {
        val rawData = mapOf(
            "iss" to "s6BhdRkqt3",
            "foo" to "bar"
        )

        val correspondingExpectedRequestObject = RequestObject(
            issuer = "s6BhdRkqt3",
            audience = emptyList(),
            scope = emptyList(),
            claims = Claims()
        )

        val requestJwt = "someHeader.${Base64.encodeBase64URLSafeString(Jackson.asFormatString(rawData).toByteArray()).replace("=", "")}.someSignature"

        assertThat(RequestObjectExtractor.extractRequestObjectFromJwt(requestJwt), equalTo(success(correspondingExpectedRequestObject)))
    }

    @Test
    fun `multiple audiences are supported`() {
        val rawData = mapOf(
            "aud" to listOf("https://audience1", "https://audience2", "https://audience3")
        )

        val correspondingExpectedRequestObject = RequestObject(
            audience = listOf("https://audience1", "https://audience2", "https://audience3"),
            scope = emptyList()
        )

        val requestJwt = "someHeader.${Base64.encodeBase64URLSafeString(Jackson.asFormatString(rawData).toByteArray()).replace("=", "")}.someSignature"

        assertThat(RequestObjectExtractor.extractRequestObjectFromJwt(requestJwt), equalTo(success(correspondingExpectedRequestObject)))
    }

    private fun failure(): Result<Map<*, *>, InvalidRequestObject> = Failure(InvalidRequestObject)
    private fun <T> success(data: T): Result<T, InvalidRequestObject> = Success(data)
}
