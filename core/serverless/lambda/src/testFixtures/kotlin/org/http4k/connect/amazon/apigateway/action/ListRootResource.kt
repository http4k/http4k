package org.http4k.connect.amazon.apigateway.action

import org.http4k.connect.amazon.apigateway.AwsApiGatewayAction
import org.http4k.connect.amazon.apigateway.model.ApiId
import org.http4k.connect.kClass
import org.http4k.core.Method
import org.http4k.core.Request

class ListRootResource(private val apiId: ApiId) : AwsApiGatewayAction<ListResourcesResponse>(kClass()) {
    override fun toRequest(): Request = Request(Method.GET, "/restapis/${apiId.value}/resources")
}
