package org.http4k.security.oauth.format

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Uri
import org.http4k.security.ResponseType
import org.http4k.security.oauth.metadata.AuthMethod.client_secret_basic
import org.http4k.security.oauth.metadata.ServerMetadata
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.Locale

@ExtendWith(JsonApprovalTest::class)
class ServerMetadataMoshiAdapterTest {
    private val marshaller = OAuthMoshi

    @Test
    fun `deserialize full metadata`() {
        val json = """
    {
        "issuer": "https://example.com",
        "authorization_endpoint": "https://example.com/auth",
        "token_endpoint": "https://example.com/token",
        "token_endpoint_auth_methods_supported": ["client_secret_basic"],
        "token_endpoint_auth_signing_alg_values_supported": ["RS256", "ES256"],
        "response_types_supported": ["Code"],
        "scopes_supported": ["openid", "email"],
        "ui_locales_supported": ["en-GB", "fr-FR"],
        "userinfo_endpoint": "https://example.com/userinfo",
        "jwks_uri": "https://example.com/jwks",
        "registration_endpoint": "https://example.com/register",
        "service_documentation": "https://example.com/docs",
        "signed_metadata": "some-signature"
    }
    """.trimIndent()

        assertThat(
            marshaller.asA(json), equalTo(
                ServerMetadata(
                    issuer = "https://example.com",
                    authorization_endpoint = Uri.of("https://example.com/auth"),
                    token_endpoint = Uri.of("https://example.com/token"),
                    token_endpoint_auth_methods_supported = listOf(client_secret_basic),
                    token_endpoint_auth_signing_alg_values_supported = listOf("RS256", "ES256"),
                    response_types_supported = listOf(ResponseType.Code),
                    scopes_supported = listOf("openid", "email"),
                    ui_locales_supported = listOf(Locale.forLanguageTag("en-GB"), Locale.forLanguageTag("fr-FR")),
                    userinfo_endpoint = Uri.of("https://example.com/userinfo"),
                    jwks_uri = Uri.of("https://example.com/jwks"),
                    registration_endpoint = Uri.of("https://example.com/register"),
                    service_documentation = Uri.of("https://example.com/docs"),
                    signed_metadata = "some-signature"
                )
            )
        )
    }

    @Test
    fun `deserialize minimal metadata`() {
        val json = """
    {
        "issuer": "https://example.com",
        "authorization_endpoint": "https://example.com/auth",
        "token_endpoint": "https://example.com/token",
        "token_endpoint_auth_methods_supported": ["client_secret_basic"],
        "token_endpoint_auth_signing_alg_values_supported": ["RS256"],
        "response_types_supported": ["Code"],
        "ui_locales_supported": ["en-GB"]
    }
    """.trimIndent()

        assertThat(
            marshaller.asA(json), equalTo(
                ServerMetadata(
                    issuer = "https://example.com",
                    authorization_endpoint = Uri.of("https://example.com/auth"),
                    token_endpoint = Uri.of("https://example.com/token"),
                    token_endpoint_auth_methods_supported = listOf(client_secret_basic),
                    token_endpoint_auth_signing_alg_values_supported = listOf("RS256"),
                    response_types_supported = listOf(ResponseType.Code),
                    ui_locales_supported = listOf(Locale.forLanguageTag("en-GB"))
                )
            )
        )
    }

    @Test
    fun `ignores unknown fields`() {
        val json = """
    {
        "issuer": "https://example.com",
        "authorization_endpoint": "https://example.com/auth",
        "token_endpoint": "https://example.com/token",
        "token_endpoint_auth_methods_supported": ["client_secret_basic"],
        "token_endpoint_auth_signing_alg_values_supported": ["RS256"],
        "response_types_supported": ["Code"],
        "ui_locales_supported": ["en-GB"],
        "unknown_field": "some value"
    }
    """.trimIndent()

        assertThat(
            marshaller.asA(json), equalTo(
                ServerMetadata(
                    issuer = "https://example.com",
                    authorization_endpoint = Uri.of("https://example.com/auth"),
                    token_endpoint = Uri.of("https://example.com/token"),
                    token_endpoint_auth_methods_supported = listOf(client_secret_basic),
                    token_endpoint_auth_signing_alg_values_supported = listOf("RS256"),
                    response_types_supported = listOf(ResponseType.Code),
                    ui_locales_supported = listOf(Locale.forLanguageTag("en-GB"))
                )
            )
        )
    }

    @Test
    fun `serialize full metadata`(approver: Approver) {
        val data = ServerMetadata(
            issuer = "https://example.com",
            authorization_endpoint = Uri.of("https://example.com/auth"),
            token_endpoint = Uri.of("https://example.com/token"),
            token_endpoint_auth_methods_supported = listOf(client_secret_basic),
            token_endpoint_auth_signing_alg_values_supported = listOf("RS256", "ES256"),
            response_types_supported = listOf(ResponseType.Code),
            scopes_supported = listOf("openid", "email"),
            ui_locales_supported = listOf(Locale.forLanguageTag("en-GB"), Locale.forLanguageTag("fr-FR")),
            userinfo_endpoint = Uri.of("https://example.com/userinfo"),
            jwks_uri = Uri.of("https://example.com/jwks"),
            registration_endpoint = Uri.of("https://example.com/register"),
            service_documentation = Uri.of("https://example.com/docs"),
            signed_metadata = "some-signature"
        )

        approver.assertApproved(marshaller.asFormatString(data), APPLICATION_JSON)
    }

    @Test
    fun `serialize minimal metadata`(approver: Approver) {
        val data = ServerMetadata(
            issuer = "https://example.com",
            authorization_endpoint = Uri.of("https://example.com/auth"),
            token_endpoint = Uri.of("https://example.com/token"),
            token_endpoint_auth_methods_supported = listOf(client_secret_basic),
            token_endpoint_auth_signing_alg_values_supported = listOf("RS256"),
            response_types_supported = listOf(ResponseType.Code),
            ui_locales_supported = listOf(Locale.forLanguageTag("en-GB"))
        )

        approver.assertApproved(marshaller.asFormatString(data), APPLICATION_JSON)
    }
}
