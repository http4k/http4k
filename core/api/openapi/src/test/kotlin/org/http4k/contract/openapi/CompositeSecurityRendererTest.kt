package org.http4k.contract.openapi

import org.http4k.contract.openapi.v3.OpenApi3SecurityRenderer
import org.http4k.contract.security.and
import org.http4k.contract.security.or

class CompositeSecurityRendererTest : SecurityRendererContract {
    override val security =
        (security("first").and(security("second")))
            .or((security("third").and(security("fourth"))))

    override val renderer = OpenApi3SecurityRenderer
}

