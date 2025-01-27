package org.http4k.contract.openapi

import org.http4k.contract.openapi.v3.OpenApi3SecurityRenderer
import org.http4k.contract.security.and

class AndSecurityRendererTest : SecurityRendererContract {
    override val security = security("first").and(security("second")).and(security("third"))

    override val renderer = OpenApi3SecurityRenderer
}
