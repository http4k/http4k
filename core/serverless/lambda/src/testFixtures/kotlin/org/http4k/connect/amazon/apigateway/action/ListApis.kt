package org.http4k.connect.amazon.apigateway.action

import org.http4k.connect.amazon.apigateway.AwsApiGatewayAction
import org.http4k.connect.kClass
import org.http4k.core.Method
import org.http4k.core.Request

class ListApis : AwsApiGatewayAction<ListApiResponse>(kClass()) {
    override fun toRequest() = Request(Method.GET, "/restapis")
}

data class ListResourcesResponse(val _embedded: EmbeddedResourceDetails)

data class ListApiResponse(val _embedded: EmbeddedDetails? = EmbeddedDetails(emptyList()))

data class EmbeddedDetails(val item: List<RestApiDetails>)

data class RestApiDetails(val name: String, val id: String)

data class RestResourceDetails(val id: String, val path: String)

data class EmbeddedResourceDetails(val item: RestResourceDetails)
