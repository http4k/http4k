package org.http4k.connect.amazon.apigatewayv2.action

import org.http4k.connect.amazon.apigatewayv2.AwsApiGatewayV2Action
import org.http4k.connect.amazon.apigatewayv2.model.ApiId
import org.http4k.connect.kClass
import org.http4k.core.Method
import org.http4k.core.Request

class DeleteApi(private val apiId: ApiId) : AwsApiGatewayV2Action<Unit>(kClass()) {
    override fun toRequest() = Request(Method.DELETE, "/v2/apis/${apiId.value}")
}
