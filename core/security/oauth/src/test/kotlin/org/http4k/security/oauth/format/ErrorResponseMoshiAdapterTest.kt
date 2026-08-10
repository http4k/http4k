package org.http4k.security.oauth.format

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.squareup.moshi.JsonDataException
import org.http4k.security.oauth.server.ErrorResponse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ErrorResponseMoshiAdapterTest {

    @Test
    fun `error with every field populated survives a JSON round trip`() {
        val error = ErrorResponse("invalid_request", "a description", "https://example.com/error")

        assertThat(OAuthMoshi.asA<ErrorResponse>(OAuthMoshi.asFormatString(error)), equalTo(error))
    }

    @Test
    fun `error without an error_uri survives a JSON round trip`() {
        val error = ErrorResponse("invalid_request", "a description", null)

        assertThat(OAuthMoshi.asA<ErrorResponse>(OAuthMoshi.asFormatString(error)), equalTo(error))
    }

    @Test
    fun `explicit nulls are treated as missing`() {
        val json = """{"error":"invalid_request","error_description":"a description","error_uri":null}"""

        assertThat(
            OAuthMoshi.asA<ErrorResponse>(json),
            equalTo(ErrorResponse("invalid_request", "a description", null))
        )
    }

    @Test
    fun `unknown fields are ignored`() {
        val json = """{"error":"invalid_request","error_description":"a description","unknown":"value"}"""

        assertThat(
            OAuthMoshi.asA<ErrorResponse>(json),
            equalTo(ErrorResponse("invalid_request", "a description", null))
        )
    }

    @Test
    fun `a missing error is rejected`() {
        assertThrows<JsonDataException> { OAuthMoshi.asA<ErrorResponse>("""{"error_description":"a description"}""") }
    }

    @Test
    fun `a missing error_description is rejected`() {
        assertThrows<JsonDataException> { OAuthMoshi.asA<ErrorResponse>("""{"error":"invalid_request"}""") }
    }
}
