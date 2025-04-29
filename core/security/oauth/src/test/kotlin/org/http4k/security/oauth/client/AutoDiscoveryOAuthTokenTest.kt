package org.http4k.security.oauth.client

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Body
import org.http4k.core.Credentials
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ClientFilters
import org.http4k.lens.Validator
import org.http4k.lens.WebForm
import org.http4k.lens.webForm
import org.http4k.security.OAuthWebForms.clientId
import org.http4k.security.OAuthWebForms.clientSecret
import org.http4k.security.OAuthWebForms.grantType
import org.http4k.security.OAuthWebForms.scope
import org.junit.jupiter.api.Test

class AutoDiscoveryOAuthTokenTest {
    private val credentials = Credentials("id", "secret")
    private val baseUri = Uri.of("https://example.com")
    private val requests = mutableListOf<Request>()
    private val scopes = listOf("read", "write")

    private val backend = { request: Request ->
        requests.add(request)
        when (request.uri.path) {
            "/.well-known/oauth-authorization-server" -> Response(OK).body(
                """
                    {
                        "issuer": "https://example.com",
                        "authorization_endpoint": "https://example.com/custom/auth",
                        "token_endpoint": "https://example.com/custom/token",
                        "token_endpoint_auth_methods_supported": ["client_secret_basic"]
                    }
                    """.trimIndent()
            )

            "/custom/token" -> Response(OK).body(
                """
                    {
                        "access_token": "test-token",
                        "token_type": "bearer",
                        "expires_in": 3600
                    }
                    """.trimIndent()
            )

            else -> Response(NOT_FOUND)
        }
    }

    @Test
    fun `uses discovered endpoints when metadata is available`() {

        val app = ClientFilters.AutoDiscoveryOAuthToken(
            authServerUri = baseUri,
            credentials = credentials,
            backend = backend,
            scopes = scopes
        ).then { Response(OK) }

        app(Request(GET, "https://api.example.com/test"))

        assertThat(
            requests[1].uri.path,
            equalTo("/custom/token")
        )

        assertThat(
            Body.webForm(Validator.Ignore).toLens().extract(requests[1]),
            equalTo(
                WebForm()
                    .with(grantType of "client_credentials")
                    .with(clientId of "id")
                    .with(clientSecret of "secret")
                    .with(scope of "read write")
            )
        )
    }

    @Test
    fun `uses default endpoints when metadata discovery fails`() {
        val backend = { request: Request ->
            requests.add(request)
            when (request.uri.path) {
                "/.well-known/oauth-authorization-server" -> Response(NOT_FOUND)
                "/token" -> Response(OK).body(
                    """
                    {
                        "access_token": "test-token",
                        "token_type": "bearer",
                        "expires_in": 3600
                    }
                    """.trimIndent()
                )

                else -> Response(NOT_FOUND)
            }
        }

        val app = ClientFilters.AutoDiscoveryOAuthToken(
            authServerUri = baseUri,
            credentials = credentials,
            backend = backend,
            scopes = scopes
        ).then { Response(OK) }

        app(Request(GET, "https://api.example.com/test"))

        assertThat(
            requests[1].uri.path,
            equalTo("/token")
        )

        assertThat(
            Body.webForm(Validator.Ignore).toLens().extract(requests[1]),
            equalTo(
                WebForm()
                    .with(grantType of "client_credentials")
                    .with(clientId of "id")
                    .with(clientSecret of "secret")
                    .with(scope of "read write")
            )
        )
    }
}
