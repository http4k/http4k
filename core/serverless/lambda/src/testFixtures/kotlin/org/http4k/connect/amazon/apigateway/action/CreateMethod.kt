package org.http4k.connect.amazon.apigateway.action

import org.http4k.connect.amazon.apigateway.ApiGatewayJackson.auto
import org.http4k.connect.amazon.apigateway.AwsApiGatewayAction
import org.http4k.connect.amazon.apigateway.model.ApiId
import org.http4k.connect.kClass
import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.with

class CreateMethod(private val apiId: ApiId, private val resource: RestResourceDetails) :
    AwsApiGatewayAction<Unit>(kClass()) {

    override fun toRequest() = Request(Method.PUT, "/restapis/${apiId.value}/resources/${resource.id}/methods/ANY")
        .with(Body.auto<CreateMethodRequest>().toLens() of CreateMethodRequest(authorizationType = "NONE"))

    private data class CreateMethodRequest(val authorizationType: String)
}
