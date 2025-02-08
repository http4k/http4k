package org.http4k.contract.openapi.v3

import org.http4k.contract.openapi.SecurityRendererContract
import org.http4k.core.Uri
import org.http4k.security.OAuthSecurity
import org.http4k.security.googleCloudEndpoints

class GoogleCloudEndpointsSecurityRendererTest : SecurityRendererContract {
    override val security = OAuthSecurity.googleCloudEndpoints("issuer", Uri.of("issuer"), listOf("first"))
    override val renderer = OpenApi3SecurityRenderer
}
