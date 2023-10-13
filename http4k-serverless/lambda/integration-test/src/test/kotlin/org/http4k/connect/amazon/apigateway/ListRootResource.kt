package org.http4k.connect.amazon.apigateway

import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.connect.amazon.apigatewayv2.model.ApiId
import org.http4k.connect.amazon.kClass

class ListRootResource(private val apiId: ApiId) : AwsApiGatewayAction<ListResourcesResponse>(kClass()) {
    override fun toRequest(): Request = Request(Method.GET, "/restapis/${apiId.value}/resources")
}
