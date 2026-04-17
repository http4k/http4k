package org.http4k.security.oauth.client

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.lens.contentType
import org.junit.jupiter.api.Test

class AuthServerDiscoveryTest {

    private fun serverMetadataResponse(issuer: String) = Response(OK)
        .contentType(APPLICATION_JSON)
        .body(
            """
            {
                "issuer": "$issuer",
                "authorization_endpoint": "https://example.com/auth",
                "token_endpoint": "https://example.com/token",
                "token_endpoint_auth_methods_supported": ["client_secret_basic"]
            }
            """.trimIndent()
        )

    private fun resourceMetadataResponse(resource: String) = Response(OK)
        .contentType(APPLICATION_JSON)
        .body(
            """
            {
                "resource": "$resource",
                "authorization_servers": ["https://example.com"]
            }
            """.trimIndent()
        )

    private fun discoveryBackend(resource: String, issuer: String = "https://example.com") = { request: Request ->
        when (request.uri.path) {
            "/.well-known/oauth-protected-resource" -> resourceMetadataResponse(resource)
            "/.well-known/oauth-authorization-server" -> serverMetadataResponse(issuer)
            else -> Response(OK)
        }
    }

    @Test
    fun `succeeds when issuer matches server URL`() {
        val serverUri = Uri.of("https://example.com")
        val discovery = AuthServerDiscovery.fromKnownAuthServer(serverUri)

        val result = discovery { serverMetadataResponse("https://example.com") }

        assertThat((result as Success).value.serverUri, equalTo(serverUri))
    }

    @Test
    fun `succeeds when issuer matches with explicit default port`() {
        val serverUri = Uri.of("https://example.com:443")
        val discovery = AuthServerDiscovery.fromKnownAuthServer(serverUri)

        val result = discovery { serverMetadataResponse("https://example.com") }

        assertThat(result is Success, equalTo(true))
    }

    @Test
    fun `fails when issuer does not match server URL`() {
        val serverUri = Uri.of("https://example.com")
        val discovery = AuthServerDiscovery.fromKnownAuthServer(serverUri)

        val result = discovery { serverMetadataResponse("https://evil.com") }

        val message = (result as Failure).reason.message!!
        assertThat(message, containsSubstring("RFC 8414"))
        assertThat(message, containsSubstring("https://evil.com"))
    }

    @Test
    fun `fails when issuer has different scheme`() {
        val serverUri = Uri.of("https://example.com")
        val discovery = AuthServerDiscovery.fromKnownAuthServer(serverUri)

        val result = discovery { serverMetadataResponse("http://example.com") }

        assertThat((result as Failure).reason.message!!, containsSubstring("RFC 8414"))
    }

    @Test
    fun `fails when issuer has different port`() {
        val serverUri = Uri.of("https://example.com")
        val discovery = AuthServerDiscovery.fromKnownAuthServer(serverUri)

        val result = discovery { serverMetadataResponse("https://example.com:8443") }

        assertThat((result as Failure).reason.message!!, containsSubstring("RFC 8414"))
    }

    @Test
    fun `fromProtectedResource succeeds when resource matches`() {
        val resourceUri = Uri.of("https://example.com/api")
        val discovery = AuthServerDiscovery.fromProtectedResource(resourceUri)

        val result = discovery(discoveryBackend("https://example.com/api"))

        assertThat(result is Success, equalTo(true))
    }

    @Test
    fun `fromProtectedResource succeeds when resource is a path prefix`() {
        val resourceUri = Uri.of("https://example.com/api/v1/items")
        val discovery = AuthServerDiscovery.fromProtectedResource(resourceUri)

        val result = discovery(discoveryBackend("https://example.com/api"))

        assertThat(result is Success, equalTo(true))
    }

    @Test
    fun `fromProtectedResource succeeds when resource is relative path`() {
        val resourceUri = Uri.of("https://example.com/api")
        val discovery = AuthServerDiscovery.fromProtectedResource(resourceUri)

        val result = discovery(discoveryBackend("/api"))

        assertThat(result is Success, equalTo(true))
    }

    @Test
    fun `fromProtectedResource fails when resource has different host`() {
        val resourceUri = Uri.of("https://example.com/api")
        val discovery = AuthServerDiscovery.fromProtectedResource(resourceUri)

        val result = discovery(discoveryBackend("https://evil.com/api"))

        val message = (result as Failure).reason.message!!
        assertThat(message, containsSubstring("RFC 9728"))
        assertThat(message, containsSubstring("https://evil.com/api"))
    }

    @Test
    fun `fromProtectedResource fails when resource path does not match`() {
        val resourceUri = Uri.of("https://example.com/api")
        val discovery = AuthServerDiscovery.fromProtectedResource(resourceUri)

        val result = discovery(discoveryBackend("https://example.com/other"))

        assertThat((result as Failure).reason.message!!, containsSubstring("RFC 9728"))
    }
}
