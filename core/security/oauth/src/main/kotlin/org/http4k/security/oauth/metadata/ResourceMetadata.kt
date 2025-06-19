package org.http4k.security.oauth.metadata

import org.http4k.core.Uri

data class ResourceMetadata(
    val resource: Uri,
    val authorizationServers: List<Uri>? = null,
    val jwksUri: Uri? = null,
    val scopesSupported: List<String>? = null,
    val bearerMethodsSupported: List<BearerMethod>? = null,
    val resourceSigningAlgValuesSupported: List<String>? = null,
    val resourceName: String? = null,
    val resourceDocumentation: Uri? = null,
    val resourcePolicyUri: Uri? = null,
    val resourceTosUri: Uri? = null,
    val tlsClientCertificateBoundAccessTokens: Boolean? = null,
    val authorizationDetailsTypesSupported: List<String>? = null,
    val dpopSigningAlgValuesSupported: List<String>? = null,
    val dpopBoundAccessTokensRequired: Boolean? = null,
    val signedMetadata: String? = null
)
