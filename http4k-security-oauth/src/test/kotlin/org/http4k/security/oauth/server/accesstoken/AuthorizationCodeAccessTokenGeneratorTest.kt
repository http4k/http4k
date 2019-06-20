package org.http4k.security.oauth.server.accesstoken

import com.natpryce.get
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.ContentType
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.body.form
import org.http4k.core.with
import org.http4k.lens.Header
import org.http4k.security.oauth.server.AuthorizationCode
import org.http4k.security.oauth.server.ClientId
import org.junit.jupiter.api.Test

internal class AuthorizationCodeAccessTokenGeneratorTest {
    @Test
    fun `grant type=authorization code - can generate request`() {
        val request = AuthorizationCodeAccessTokenGenerator.extract(Request(Method.POST, "/irrelevant")
            .with(Header.CONTENT_TYPE of ContentType.APPLICATION_FORM_URLENCODED)
            .form("redirect_uri", "http://some-uri")
            .form("grant_type", "authorization_code")
            .form("code", "some-code")
            .form("client_id", "a-client-id")
            .form("client_secret", "a-client-secret")).get()
            as? AuthorizationCodeAccessTokenRequest ?: org.junit.jupiter.api.fail("returned wrong type")

        assertThat(request, equalTo(AuthorizationCodeAccessTokenRequest(
            ClientId("a-client-id"),
            "a-client-secret",
            Uri.of("http://some-uri"),
            AuthorizationCode("some-code")
        )))
    }
}