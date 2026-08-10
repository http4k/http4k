package org.http4k.security.oauth.format

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Uri
import org.http4k.security.oauth.metadata.BearerMethod.body
import org.http4k.security.oauth.metadata.BearerMethod.header
import org.http4k.security.oauth.metadata.BearerMethod.query
import org.http4k.security.oauth.metadata.ResourceMetadata
import org.junit.jupiter.api.Test

class ResourceMetadataMoshiAdapterTest {

    @Test
    fun `metadata with every field populated survives a JSON round trip`() {
        val metadata = ResourceMetadata(
            resource = Uri.of("https://example.com/resource"),
            authorizationServers = listOf(Uri.of("https://example.com/as1"), Uri.of("https://example.com/as2")),
            jwksUri = Uri.of("https://example.com/jwks"),
            scopesSupported = listOf("read", "write"),
            bearerMethodsSupported = listOf(header, body, query),
            resourceSigningAlgValuesSupported = listOf("RS256", "ES256"),
            resourceName = "a resource",
            resourceDocumentation = Uri.of("https://example.com/docs"),
            resourcePolicyUri = Uri.of("https://example.com/policy"),
            resourceTosUri = Uri.of("https://example.com/tos"),
            tlsClientCertificateBoundAccessTokens = true,
            authorizationDetailsTypesSupported = listOf("payment_initiation"),
            dpopSigningAlgValuesSupported = listOf("PS256"),
            dpopBoundAccessTokensRequired = false,
            signedMetadata = "a-signature"
        )

        assertThat(OAuthMoshi.asA<ResourceMetadata>(OAuthMoshi.asFormatString(metadata)), equalTo(metadata))
    }

    @Test
    fun `metadata with only the required resource survives a JSON round trip`() {
        val metadata = ResourceMetadata(resource = Uri.of("https://example.com/resource"))

        assertThat(OAuthMoshi.asA<ResourceMetadata>(OAuthMoshi.asFormatString(metadata)), equalTo(metadata))
    }

    @Test
    fun `bearer methods are read case-insensitively`() {
        val json = """{"resource":"https://example.com/resource","bearer_methods_supported":["HEADER","Body"]}"""

        assertThat(
            OAuthMoshi.asA<ResourceMetadata>(json),
            equalTo(
                ResourceMetadata(
                    resource = Uri.of("https://example.com/resource"),
                    bearerMethodsSupported = listOf(header, body)
                )
            )
        )
    }
}
