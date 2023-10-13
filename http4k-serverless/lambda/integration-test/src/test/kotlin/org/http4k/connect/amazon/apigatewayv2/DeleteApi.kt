package org.http4k.connect.amazon.apigatewayv2

import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.connect.amazon.kClass

class DeleteApi(private val apiId: ApiId) : AwsApiGatewayV2Action<Unit>(kClass()) {
    override fun toRequest() = Request(Method.DELETE, "/v2/apis/${apiId.value}")
}
