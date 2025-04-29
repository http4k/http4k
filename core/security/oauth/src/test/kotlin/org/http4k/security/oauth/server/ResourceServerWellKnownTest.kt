package org.http4k.security.oauth.server

import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.security.oauth.metadata.BearerMethod.body
import org.http4k.security.oauth.metadata.BearerMethod.header
import org.http4k.security.oauth.metadata.ResourceMetadata
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(JsonApprovalTest::class)
class ResourceServerWellKnownTest {

    private val resource = ResourceMetadata(
        Uri.of("https://api.example.com"),
        listOf(Uri.of("https://auth.example.com"), Uri.of("https://login.example.org")),
        Uri.of("https://api.example.com/.well-known/jwks.json"), listOf("read", "write", "delete", "admin"),
        listOf(header, body),
        listOf("RS256", "ES256", "PS256"),
        "Example API Service",
        Uri.of("https://docs.example.com/api"),
        Uri.of("https://api.example.com/policy"), Uri.of("https://api.example.com/terms"),
        true,
        listOf("payment", "account_info", "subscription"),
        listOf("ES256", "RS256"),
        false,
        "eyJhbGciOiJSUzI1NiIsImtpZCI6ImV4YW1wbGUta2V5LWlkIn0.eyJyZXNvdXJjZSI6Imh0dHBzOi8vYXBpLmV4YW1wbGUuY29tIiwiYXV0aG9yaXphdGlvbl9zZXJ2ZXJzIjpbImh0dHBzOi8vYXV0aC5leGFtcGxlLmNvbSIsImh0dHBzOi8vbG9naW4uZXhhbXBsZS5vcmciXSwic2NvcGVzX3N1cHBvcnRlZCI6WyJyZWFkIiwid3JpdGUiLCJkZWxldGUiLCJhZG1pbiJdLCJpc3MiOiJodHRwczovL2FwaS5leGFtcGxlLmNvbSJ9.signature"
    )

    val http = ResourceServerWellKnown(resource)

    @Test
    fun `gets resource metadata`(approver: Approver) {
        approver.assertApproved(
            http(Request(GET, "/.well-known/oauth-protected-resource"))
        )
    }
}
