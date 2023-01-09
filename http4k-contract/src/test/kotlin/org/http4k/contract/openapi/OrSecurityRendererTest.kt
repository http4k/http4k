package org.http4k.contract.openapi

import org.http4k.contract.openapi.v3.OpenApi3SecurityRenderer
import org.http4k.contract.security.or

class OrSecurityRendererTest : SecurityRendererContract {
    override val security = security("first").or(security("second")).or(security("third"))
    override val renderer = OpenApi3SecurityRenderer
}
