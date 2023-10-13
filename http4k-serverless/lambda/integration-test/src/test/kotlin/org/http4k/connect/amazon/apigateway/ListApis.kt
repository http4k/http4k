package org.http4k.connect.amazon.apigateway

import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.connect.amazon.kClass

class ListApis : AwsApiGatewayAction<ListApiResponse>(kClass()) {
    override fun toRequest() = Request(Method.GET, "/restapis")
}
