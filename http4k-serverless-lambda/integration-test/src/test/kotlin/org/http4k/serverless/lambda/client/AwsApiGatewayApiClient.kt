package org.http4k.serverless.lambda.client

import org.http4k.core.Body
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.format.Jackson.auto

class AwsApiGatewayApiClient(rawClient: HttpHandler, region: Region) {
    private val client = ApiGatewayApi(region).then(rawClient)

    fun create(name: String): ApiDetails =
        ApiDetails.lens(client(Request(Method.POST, "/v2/apis").with(createApiLens of Api(name))))

    fun listApis(): List<ApiDetails> = listApiLens(client(Request(Method.GET, "/v2/apis"))).items

    fun delete(apiId: String) {
        client(Request(Method.DELETE, "/v2/apis/$apiId"))
    }

    companion object {
        private val createApiLens = Body.auto<Api>().toLens()
        private val listApiLens = Body.auto<ListApiResponse>().toLens()
    }

    private data class Api(val name: String, val protocolType: String = "HTTP")

    private data class ListApiResponse(val items: List<ApiDetails>)

    object ApiGatewayApi {
        operator fun invoke(region: Region): Filter = Filter { next ->
            { request -> next(request.uri(request.uri.host("apigateway.${region.name}.amazonaws.com").scheme("https"))) }
        }
    }
}


data class Stage(val stageName: String, val autoDeploy: Boolean = true) {
    companion object {
        val lens = Body.auto<Stage>().toLens()
    }
}

data class ApiDetails(val name: String, val apiId: String, val apiEndpoint: String) {
    companion object {
        val lens = Body.auto<ApiDetails>().toLens()
    }
}


data class Integration(
    val integrationType: String = "AWS_PROXY",
    val integrationUri: String,
    val timeoutInMillis: Long = 30000,
    val payloadFormatVersion: String = "1.0"
) {
    companion object {
        val lens = Body.auto<Integration>().toLens()
    }
}

data class IntegrationInfo(val integrationId: String) {
    companion object {
        val lens = Body.auto<IntegrationInfo>().toLens()
    }
}

data class Route(val target: String, val routeKey: String = "\$default") {
    companion object {
        val lens = Body.auto<Route>().toLens()
    }
}
