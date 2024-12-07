package org.http4k.connect.amazon.apigateway.action

import org.http4k.connect.amazon.apigateway.AwsApiGatewayAction
import org.http4k.connect.amazon.apigateway.model.ApiId
import org.http4k.connect.kClass
import org.http4k.core.Method
import org.http4k.core.Request

class DeleteApi(private val apiId: ApiId) : AwsApiGatewayAction<Unit>(kClass()) {
    override fun toRequest() = Request(Method.DELETE, "/restapis/${apiId.value}")
}
