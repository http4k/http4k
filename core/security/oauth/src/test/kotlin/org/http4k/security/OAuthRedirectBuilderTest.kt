package org.http4k.security

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Uri
import org.http4k.security.oauth.server.AuthRequest
import org.http4k.security.oauth.server.ClientId
import org.junit.jupiter.api.Test

class OAuthRedirectBuilderTest {

    private val authUri = Uri.of("https://example.com/auth")
    private val state = State("s")

    private fun authRequest(codeChallenge: String? = null) = AuthRequest(
        client = ClientId("a-client"),
        scopes = listOf("openid"),
        redirectUri = Uri.of("https://app.example.com/callback"),
        state = state,
        codeChallenge = codeChallenge
    )

    @Test
    fun `emits code_challenge_method=S256 when challenge is supplied`() {
        val uri = defaultUriBuilder(authUri, authRequest(codeChallenge = "abc"), state, null)

        assertThat(uri.toString(), containsSubstring("code_challenge=abc"))
        assertThat(uri.toString(), containsSubstring("code_challenge_method=S256"))
    }

    @Test
    fun `omits both pkce query params when codeChallenge is null`() {
        val uri = defaultUriBuilder(authUri, authRequest(codeChallenge = null), state, null)

        assertThat(uri.toString().contains("code_challenge"), equalTo(false))
        assertThat(uri.toString().contains("code_challenge_method"), equalTo(false))
    }
}
