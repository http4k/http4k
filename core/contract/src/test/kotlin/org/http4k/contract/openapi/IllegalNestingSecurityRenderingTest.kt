package org.http4k.contract.openapi

import org.http4k.contract.openapi.v3.OpenApi3SecurityRenderer
import org.http4k.contract.security.and
import org.http4k.contract.security.or

class IllegalNestingSecurityRenderingTest : SecurityRendererContract {
    // this is illegal because OR securities can only exist on the top level
    // the rendering will come out as empty because every security has to have
    // a name (and ORs are an array instead of an object)
    override val security =
        (security("first").or(security("second")))
            .and((security("third").or(security("fourth"))))

    override val renderer = OpenApi3SecurityRenderer
}
