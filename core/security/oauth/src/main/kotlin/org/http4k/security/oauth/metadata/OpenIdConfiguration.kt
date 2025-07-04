package org.http4k.security.oauth.metadata

import org.http4k.core.Uri
import org.http4k.security.ResponseType

data class OpenIdConfiguration(
    val issuer: Uri,
    val authorizationEndpoint: Uri,
    val tokenEndpoint: Uri,
    val jwksUri: Uri,
    val responseTypesSupported: List<ResponseType>,
    val subjectTypesSupported: List<String>,
    val idTokenSigningAlgValuesSupported: List<String>,

    val userinfoEndpoint: Uri? = null,
    val registrationEndpoint: Uri? = null,
    val scopesSupported: List<String>? = null,
    val claimsSupported: List<String>? = null,
    val grantTypesSupported: List<String>? = null,
    val tokenEndpointAuthMethodsSupported: List<String>? = null,
    val tokenEndpointAuthSigningAlgValuesSupported: List<String>? = null,
    val serviceDocumentation: Uri? = null,
    val uiLocalesSupported: List<String>? = null,

    // Session management related fields
    val endSessionEndpoint: Uri? = null,
    val checkSessionIframe: Uri? = null,

    // Other optional fields
    val revocationEndpoint: Uri? = null,
    val introspectionEndpoint: Uri? = null,
    val claimsParameterSupported: Boolean? = null,
    val requestParameterSupported: Boolean? = null,
    val requestUriParameterSupported: Boolean? = null,
    val requireRequestUriRegistration: Boolean? = null,
    val opPolicyUri: Uri? = null,
    val opTosUri: Uri? = null,
    val codeChallengeMethodsSupported: List<String>? = null,
    val idTokenEncryptionAlgValuesSupported: List<String>? = null,
    val idTokenEncryptionEncValuesSupported: List<String>? = null,
    val userinfoSigningAlgValuesSupported: List<String>? = null,
    val userinfoEncryptionAlgValuesSupported: List<String>? = null,
    val userinfoEncryptionEncValuesSupported: List<String>? = null,
    val requestObjectSigningAlgValuesSupported: List<String>? = null,
    val requestObjectEncryptionAlgValuesSupported: List<String>? = null,
    val requestObjectEncryptionEncValuesSupported: List<String>? = null,
    val backchannelLogoutSupported: Boolean? = null,
    val backchannelLogoutSessionSupported: Boolean? = null,

    // Additional custom properties might be included by some providers
    val additional: Map<String, Any?> = emptyMap()
)
