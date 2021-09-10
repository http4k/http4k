package org.http4k.security.oauth.client

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Credentials
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.security.OAuthProviderConfig
import org.http4k.security.oauth.core.RefreshToken
import org.junit.jupiter.api.Test

private val config = OAuthProviderConfig(Uri.of("http://auth"), "/authorize", "/", Credentials("hello", "world"))

class OAuthUserCredentialsTest {
    @Test
    fun `auths with correct form`() {
        val app = ClientFilters.OAuthUserCredentials(config, Credentials("user", "password"))
            .then { req: Request -> Response(OK).body(req.bodyString()) }
        assertThat(app(Request(POST, "")).bodyString(), equalTo("grant_type=password&client_id=hello&client_secret=world&username=user&password=password"))
    }
}

class OAuthClientCredentialsTest {
    @Test
    fun `auths with correct form`() {
        val app = ClientFilters.OAuthClientCredentials(config)
            .then { req: Request -> Response(OK).body(req.bodyString()) }
        assertThat(app(Request(POST, "")).bodyString(), equalTo("grant_type=client_credentials&client_id=hello&client_secret=world"))
    }
}

class OAuthRefreshTokenTest {
    @Test
    fun `auths with correct form`() {
        val app = ClientFilters.OAuthRefreshToken(config, RefreshToken("goodbye"))
            .then { req: Request -> Response(OK).body(req.bodyString()) }
        assertThat(app(Request(POST, "")).bodyString(), equalTo("grant_type=refresh_token&client_id=hello&client_secret=world&refresh_token=goodbye"))
    }
}
