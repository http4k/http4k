package org.http4k.security.oauth.client

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Credentials
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.security.OAuthProviderConfig
import org.junit.jupiter.api.Test

class OAuthUserCredentialsTest {
    private val config = OAuthProviderConfig(Uri.of("http://auth"), "/authorize", "/", Credentials("hello", "world"))

    @Test
    fun `auths with correct form`() {
        val app = ClientFilters.OAuthUserCredentials(
            config,
            Credentials("user", "password"),
            scopes = listOf("someactivity.write", "someactivity.read")
        ).then { req: Request -> Response(Status.OK).body(req.bodyString()) }
        assertThat(
            app(Request(Method.POST, "")).bodyString(),
            equalTo(
                "grant_type=password" +
                    "&client_id=hello" +
                    "&client_secret=world" +
                    "&username=user" +
                    "&password=password" +
                    "&scope=someactivity.write+someactivity.read"
            )
        )
    }

    @Test
    fun `omits scope if not provided`() {
        val app = ClientFilters.OAuthUserCredentials(
            config,
            Credentials("user", "password"),
            scopes = emptyList()
        ).then { req: Request -> Response(Status.OK).body(req.bodyString()) }
        assertThat(
            app(Request(Method.POST, "")).bodyString(),
            equalTo("grant_type=password&client_id=hello&client_secret=world&username=user&password=password")
        )
    }
}
