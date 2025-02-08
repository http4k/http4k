package org.http4k.security

import org.http4k.core.Filter
import org.http4k.core.Uri

class OpenIdConnectSecurity(
    val discoveryUrl: Uri,
    override val filter: Filter,
    val name: String = "openIdConnect"
) : Security {
    companion object
}
