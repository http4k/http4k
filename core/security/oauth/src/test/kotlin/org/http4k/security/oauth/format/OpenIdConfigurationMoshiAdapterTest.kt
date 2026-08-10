package org.http4k.security.oauth.format

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Uri
import org.http4k.security.ResponseType.Code
import org.http4k.security.ResponseType.CodeIdToken
import org.http4k.security.ResponseType.CodeToken
import org.http4k.security.ResponseType.Token
import org.http4k.security.oauth.metadata.OpenIdConfiguration
import org.junit.jupiter.api.Test

class OpenIdConfigurationMoshiAdapterTest {

    @Test
    fun `configuration with every field populated survives a JSON round trip`() {
        val configuration = OpenIdConfiguration(
            issuer = Uri.of("https://example.com"),
            authorizationEndpoint = Uri.of("https://example.com/auth"),
            tokenEndpoint = Uri.of("https://example.com/token"),
            jwksUri = Uri.of("https://example.com/jwks"),
            responseTypesSupported = listOf(Code, Token, CodeIdToken, CodeToken),
            subjectTypesSupported = listOf("public", "pairwise"),
            idTokenSigningAlgValuesSupported = listOf("RS256", "ES256"),

            userinfoEndpoint = Uri.of("https://example.com/userinfo"),
            registrationEndpoint = Uri.of("https://example.com/register"),
            scopesSupported = listOf("openid", "email"),
            claimsSupported = listOf("sub", "iss"),
            grantTypesSupported = listOf("authorization_code", "refresh_token"),
            tokenEndpointAuthMethodsSupported = listOf("client_secret_basic", "client_secret_post"),
            tokenEndpointAuthSigningAlgValuesSupported = listOf("RS256", "HS256"),
            serviceDocumentation = Uri.of("https://example.com/docs"),
            uiLocalesSupported = listOf("en-GB", "fr-FR"),

            endSessionEndpoint = Uri.of("https://example.com/logout"),
            checkSessionIframe = Uri.of("https://example.com/session"),

            revocationEndpoint = Uri.of("https://example.com/revoke"),
            introspectionEndpoint = Uri.of("https://example.com/introspect"),
            claimsParameterSupported = true,
            requestParameterSupported = false,
            requestUriParameterSupported = true,
            requireRequestUriRegistration = false,
            opPolicyUri = Uri.of("https://example.com/policy"),
            opTosUri = Uri.of("https://example.com/tos"),
            codeChallengeMethodsSupported = listOf("S256", "plain"),
            idTokenEncryptionAlgValuesSupported = listOf("RSA-OAEP"),
            idTokenEncryptionEncValuesSupported = listOf("A128GCM"),
            userinfoSigningAlgValuesSupported = listOf("RS256"),
            userinfoEncryptionAlgValuesSupported = listOf("RSA1_5"),
            userinfoEncryptionEncValuesSupported = listOf("A256GCM"),
            requestObjectSigningAlgValuesSupported = listOf("PS256"),
            requestObjectEncryptionAlgValuesSupported = listOf("ECDH-ES"),
            requestObjectEncryptionEncValuesSupported = listOf("A192GCM"),
            backchannelLogoutSupported = true,
            backchannelLogoutSessionSupported = false
        )

        val roundTripped = OAuthMoshi.asA<OpenIdConfiguration>(OAuthMoshi.asFormatString(configuration))

        assertThat(roundTripped, equalTo(configuration))
    }
}
