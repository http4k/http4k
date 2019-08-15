package org.http4k.contract.openapi.v2

import org.http4k.contract.openapi.SecurityRendererContract
import org.http4k.contract.security.ApiKeySecurity
import org.http4k.contract.security.BasicAuthSecurity
import org.http4k.core.Credentials
import org.http4k.lens.Query

class BasicSecurityRendererTest : SecurityRendererContract {
    override val security = BasicAuthSecurity("realm", Credentials("user", "password"))
    override val renderer = OpenApi2SecurityRenderer
}

class ApiKeySecurityRendererTest : SecurityRendererContract {
    override val security = ApiKeySecurity(Query.required("the_api_key"), { true })
    override val renderer = OpenApi2SecurityRenderer
}
