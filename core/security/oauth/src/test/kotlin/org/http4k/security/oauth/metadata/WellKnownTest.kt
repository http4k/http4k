package org.http4k.security.oauth.metadata

import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.security.ResponseType
import org.http4k.security.oauth.metadata.BearerMethod.body
import org.http4k.security.oauth.metadata.BearerMethod.header
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.Locale

@ExtendWith(JsonApprovalTest::class)
class WellKnownTest {

    private val server = ServerMetadata(
        issuer = "https://example.com",
        authorization_endpoint = Uri.of("https://example.com/auth"),
        token_endpoint = Uri.of("https://example.com/token"),
        token_endpoint_auth_methods_supported = listOf(AuthMethod.client_secret_basic),
        token_endpoint_auth_signing_alg_values_supported = listOf("RS256"),
        response_types_supported = listOf(ResponseType.Code),
        ui_locales_supported = listOf(Locale.forLanguageTag("en-GB"))
    )

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

    val keySet = JsonWebKeySet(
        listOf(
            JsonWebKey(
                kty = "RSA",
                use = "sig",
                kid = "key-id-1",
                alg = "RS256",
                n = "0vx7agoebGcQSuuPiLJXZptN9nndrQmbXEps2aiAFbWhM78LhWx4cbbfAAtVT86zwu1RK7aPFFxuhDR1L6tSoc_BJECPebWKRXjBZCiFV4n3oknjhMstn64tZ_2W-5JsGY4Hc5n9yBXArwl93lqt7_RN5w6Cf0h4QyQ5v-65YGjQR0_FDW2QvzqY368QQMicAtaSqzs8KJZgnYb9c7d0zgdAZHzu6qMQvRL5hajrn1n91CbOpbISD08qNLyrdkt-bFTWhAI4vMQFh6WeZu0fM4lFd2NcRwr3XPksINHaQ-G_xBniIqbw0Ls1jF44-csFCur-kEgU8awapJzKnqDKgw",
                e = "AQAB"
            ), JsonWebKey(
                kty = "RSA",
                use = "sig",
                kid = "key-id-2",
                alg = "RS256",
                n = "1MEoaTyRirMxnvP9lVj8uvgxMFvxfkbODXnEL7-KlzJ0Fd3fkYU4wMXndnEUXKpVOIQaZ4Xjw74fvhd3771Lk8JfX5fN0li70xvD8H3zPGDGxPQKvZqmUJQzJD822xpVjfgaJRPL4pIMsXBqHc7MTDA3bsLVSLVGG1bBfE-OAW-hgb0WMe0aA3U3jBv9xBGxJWzCewUYyrkdnPMqKpwn4CQgJJPGf83yPmBxSX1aK6nELJVZWGSTYjk-xneMZLEFGiMy6kgP5PQxZ7c4XIA_1lXfwH1xLMp5vXe5nwJJ3QlZGJYJeW7kDBmJ4xGb0LExs8CwgZKle8OMTVFUhrOrPQ",
                e = "AQAB"
            ),
            JsonWebKey(
                kty = "RSA",
                use = "sig",
                kid = "cert-key-1",
                alg = "RS256",
                x5c = listOf(
                    "MIIDQjCCAiqgAwIBAgIGATz/FuLiMA0GCSqGSIb3DQEBBQUAMGIxCzAJBgNVBAYTAlVTMQswCQYDVQQIEwJDTzEPMA0GA1UEBxMGRGVudmVyMRwwGgYDVQQKExNQaW5nIElkZW50aXR5IENvcnAuMRcwFQYDVQQDEw5CcmlhbiBDYW1wYmVsbDAeFw0xMzAyMjEyMzI5MTVaFw0xODA4MTQyMjI5MTVaMGIxCzAJBgNVBAYTAlVTMQswCQYDVQQIEwJDTzEPMA0GA1UEBxMGRGVudmVyMRwwGgYDVQQKExNQaW5nIElkZW50aXR5IENvcnAuMRcwFQYDVQQDEw5CcmlhbiBDYW1wYmVsbDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAL64zn8/QnHYMeZ0LncoXaEde1fiLm1jHjmQsF/449IYALM9if6amFtPDy2yvz3YlRij66s5gyLCyO7ANuVRJx1NbgizcAblIgjtdf/u3WG7K+IiZhtELto/A7Fck9Ws6SQvzRvOE8uSirYbgmj6He4IyMOtxyIyUXjW+yg1yuRHqcOFb88g3u0U59 OH7NlL3HyLSMBa6+P9IOF1SICPWeNGV3FBOSOGAcVYCj/H7sDUEsGvZXVZcBrFoS/g9BfNBkoUQ3V1WeqOckXCy+4T6JyvkVP/3orGAdHMrLGjS3aKSZXkOFLnN2Z7j4ZkSW8+WxjIdGjZfioOYRHkHZT0Md8CAwEAAaNPME0wCwYDVR0PBAQDAgLkMB0GA1UdJQQWMBQGCCsGAQUFBwMCBggrBgEFBQcDATAdBgNVHQ4EFgQU4E3/mCR/OZ1n5vMiYHpnC86Y/9owDQYJKoZIhvcNAQEFBQADggEBAGQqKiENKaR/Zj6GkuugnKjF2Bk6+VZxHP+quilz6V6PUHmHxJZZpQVaSfIm4JpSZ+tEUZ/GDLrFk3Qe1ELeFXRhszaS5KPxKMw0a5DaBQoClE+HGNVKMxGUW9EkWRDcEPQWLcS83/oIGBWyX9l/8h5jVh4xYZiYQNkNKv2FbGOQEX7J7CcWyXhKVyB3IP9jzK0YQ0AD3LVNKbkWL/roKb2uLBQfvNpvMxdLUK4nPG7Bj5eSDftdpOdwsC/XjLLoDZ2s+0ET9/OKvCDK55ro0cKHeVry8augTWfOg/o1+buYPV1s+0WQkXzcmS/KK3O1UqYGrEAnmr1hyZvlLHs="
                ),
                x5t = "dGhpcyBpcyBhIHRlc3QgdmFsdWU=",
                `x5t#S256` = "YW5vdGhlciB0ZXN0IHZhbHVlIGZvciB4NXQjUzI1Ng=="
            )
        )
    )

    val http = WellKnown(server, resource, keySet)

    @Test
    fun `gets server metadata`(approver: Approver) {
        approver.assertApproved(
            http(Request(GET, "/.well-known/oauth-authorization-server"))
        )
    }

    @Test
    fun `gets resource metadata`(approver: Approver) {
        approver.assertApproved(
            http(Request(GET, "/.well-known/oauth-protected-resource"))
        )
    }

    @Test
    fun `gets jwks`(approver: Approver) {
        approver.assertApproved(
            http(Request(GET, "/.well-known/jwks.json"))
        )
    }
}
