package org.http4k.security.oauth.server.accesstoken

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.get
import org.http4k.core.Uri
import org.http4k.security.oauth.server.AccessTokenError
import org.http4k.security.oauth.server.AuthorizationCode
import org.http4k.security.oauth.server.ClientId
import org.http4k.security.oauth.server.MissingAuthorizationCode
import org.http4k.security.oauth.server.MissingRedirectUri
import org.http4k.security.oauth.server.TokenRequest
import org.junit.jupiter.api.Test

internal class AuthorizationCodeAccessTokenGeneratorTest {
    @Test
    fun `grant type=authorization code - can generate request`() {
        val request = AuthorizationCodeAccessTokenGenerator.extract(
            ClientId("a-client-id"),
            TokenRequest(
                GrantType.AuthorizationCode,
                null,
                "a-client-secret",
                "some-code",
                Uri.of("http://some-uri"),
                emptyList(),
                null,
                null,
                null,
                null
            )).get()
            as? AuthorizationCodeAccessTokenRequest ?: org.junit.jupiter.api.fail("returned wrong type")

        assertThat(request, equalTo(AuthorizationCodeAccessTokenRequest(
            ClientId("a-client-id"),
            "a-client-secret",
            Uri.of("http://some-uri"),
            emptyList(),
            AuthorizationCode("some-code")
        )))
    }

    @Test
    fun `grant type=authorization code - copes with missing redirect uri`() {
        val error = AuthorizationCodeAccessTokenGenerator.extract(
            ClientId("a-client-id"),
            TokenRequest(
                GrantType.AuthorizationCode,
                null,
                "a-client-secret",
                "some-code",
                null,
                emptyList(),
                null,
                null,
                null,
                null
            )).get()
            as? AccessTokenError ?: org.junit.jupiter.api.fail("returned wrong type")

        assertThat(error, equalTo(MissingRedirectUri as AccessTokenError))
    }

    @Test
    fun `grant type=authorization code - copes with missing code`() {
        val error = AuthorizationCodeAccessTokenGenerator.extract(
            ClientId("a-client-id"),
            TokenRequest(
                GrantType.AuthorizationCode,
                null,
                "a-client-secret",
                null,
                Uri.of("http://some-uri"),
                emptyList(),
                null,
                null,
                null,
                null
            )).get()
            as? AccessTokenError ?: org.junit.jupiter.api.fail("returned wrong type")

        assertThat(error, equalTo(MissingAuthorizationCode as AccessTokenError))
    }
}
