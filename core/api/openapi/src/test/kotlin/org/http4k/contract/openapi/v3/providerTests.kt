package org.http4k.contract.openapi.v3

import org.http4k.contract.openapi.SecurityRendererContract
import org.http4k.contract.security.OAuthSecurity
import org.http4k.contract.security.googleCloudEndpoints
import org.http4k.core.Uri

class GoogleCloudEndpointsSecurityRendererTest : SecurityRendererContract {
    override val security = OAuthSecurity.googleCloudEndpoints("issuer", Uri.of("issuer"), listOf("first"))
    override val renderer = OpenApi3SecurityRenderer
}
