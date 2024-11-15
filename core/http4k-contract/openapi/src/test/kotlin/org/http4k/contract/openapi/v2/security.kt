package org.http4k.contract.openapi.v2

import org.http4k.contract.openapi.SecurityRendererContract
import org.http4k.contract.security.ApiKeySecurity
import org.http4k.contract.security.BasicAuthSecurity
import org.http4k.core.Credentials
import org.http4k.lens.Header
import org.http4k.lens.Query
import org.http4k.lens.string

class BasicSecurityRendererTest : SecurityRendererContract {
    override val security = BasicAuthSecurity("realm", Credentials("user", "password"))
    override val renderer = OpenApi2SecurityRenderer
}

class ApiKeySecurityRendererTest : SecurityRendererContract {
    override val security = ApiKeySecurity(Query.required("the_api_key"), { true })
    override val renderer = OpenApi2SecurityRenderer
}

class ApiKeySecurityWithConsumerRendererTest : SecurityRendererContract {
    override val security = ApiKeySecurity(Query.required("the_api_key"), Header.string().required("consumer-name"), { "" })
    override val renderer = OpenApi2SecurityRenderer
}
