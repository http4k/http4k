package org.http4k.connect.amazon.apigatewayv2.action

import org.http4k.connect.amazon.apigatewayv2.AwsApiGatewayV2Action
import org.http4k.connect.amazon.apigatewayv2.model.ApiDetails
import org.http4k.connect.kClass
import org.http4k.core.Method
import org.http4k.core.Request

class ListApis : AwsApiGatewayV2Action<ListApiResponse>(kClass()) {
    override fun toRequest() = Request(Method.GET, "/v2/apis")
}

data class ListApiResponse(val items: List<ApiDetails>)
