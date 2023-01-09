package org.http4k.contract.openapi

import org.http4k.contract.openapi.v3.OpenApi3SecurityRenderer
import org.http4k.contract.security.and
import org.http4k.contract.security.or

class CompositeSecurityRenderer2Test : SecurityRendererContract {
    override val security =
        (security("first").or(security("second")))
            .and((security("third").or(security("fourth"))))

    override val renderer = OpenApi3SecurityRenderer
}
