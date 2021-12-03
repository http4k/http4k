package org.http4k.contract.security

import org.http4k.core.Filter
import org.http4k.core.NoOp
import org.http4k.core.Uri

fun OAuthSecurity.Companion.googleCloudEndpoints(issuer: String, jwksUri: Uri, audiences: List<String>) = ImplicitOAuthSecurity(
    Uri.of(""),
    emptyList(),
    Filter.NoOp,
    "googleCloudEndpointsOAuth",
    null,
    mapOf(
        "x-google-issuer" to issuer,
        "x-google-jwks_uri" to jwksUri.toString(),
        "x-google-audiences" to audiences.joinToString(",")
    )
)
