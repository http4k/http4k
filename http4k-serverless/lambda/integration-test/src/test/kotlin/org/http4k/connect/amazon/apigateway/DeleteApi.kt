package org.http4k.connect.amazon.apigateway

import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.connect.amazon.apigatewayv2.model.ApiId
import org.http4k.connect.amazon.kClass

class DeleteApi(private val apiId: ApiId) : AwsApiGatewayAction<Unit>(kClass()) {
    override fun toRequest() = Request(Method.DELETE, "/restapis/${apiId.value}")
}
