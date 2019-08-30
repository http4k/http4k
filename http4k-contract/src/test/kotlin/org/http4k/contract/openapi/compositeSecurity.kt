package org.http4k.contract.openapi

import org.http4k.contract.openapi.v3.OpenApi3SecurityRenderer
import org.http4k.contract.security.ApiKeySecurity
import org.http4k.contract.security.and
import org.http4k.contract.security.or
import org.http4k.lens.Query

class AndSecurityRendererTest : SecurityRendererContract {
    override val security =
        ApiKeySecurity(Query.required("first"), { true }, name = "first")
            .and(ApiKeySecurity(Query.required("second"), { true }, name = "second"))

    override val renderer = OpenApi3SecurityRenderer
}

class OrSecurityRendererTest : SecurityRendererContract {
    override val security =
        ApiKeySecurity(Query.required("first"), { true }, name = "first")
            .or(ApiKeySecurity(Query.required("second"), { true }, name = "second"))

    override val renderer = OpenApi3SecurityRenderer
}
