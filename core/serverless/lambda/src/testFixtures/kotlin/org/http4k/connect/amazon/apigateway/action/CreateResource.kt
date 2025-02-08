package org.http4k.connect.amazon.apigateway.action

import org.http4k.connect.amazon.apigateway.ApiGatewayJackson.auto
import org.http4k.connect.amazon.apigateway.AwsApiGatewayAction
import org.http4k.connect.amazon.apigateway.model.ApiId
import org.http4k.connect.kClass
import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.with

class CreateResource(private val apiId: ApiId, private val parentResource: RestResourceDetails) : AwsApiGatewayAction<RestResourceDetails>(kClass()) {
    override fun toRequest() = Request(Method.POST, "/restapis/${apiId.value}/resources/${parentResource.id}")
        .with(Body.auto<CreateProxyResource>().toLens() of CreateProxyResource())

    private data class CreateProxyResource(val pathPart: String = "{proxy+}")
}
