package org.http4k.contract.openapi

import org.http4k.contract.openapi.v3.OpenApi3SecurityRenderer
import org.http4k.contract.security.ApiKeySecurity
import org.http4k.contract.security.and
import org.http4k.lens.Query

class AndSecurityRendererTest : SecurityRendererContract {
    override val security =
        ApiKeySecurity(Query.required("first"), { true }, name = "first")
            .and(ApiKeySecurity(Query.required("second"), { true }, name = "second"))
            .and(ApiKeySecurity(Query.required("third"), { true }, name = "third"))

    override val renderer = OpenApi3SecurityRenderer
}
