package org.http4k.serverless.lambda

import org.http4k.aws.AwsCredentialScope
import org.http4k.client.JavaHttpClient
import org.http4k.cloudnative.env.Environment
import org.http4k.cloudnative.env.EnvironmentKey
import org.http4k.core.Body
import org.http4k.core.Filter
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.AwsAuth
import org.http4k.filter.ClientFilters
import org.http4k.filter.DebuggingFilters
import org.http4k.format.Jackson.auto
import org.http4k.serverless.lambda.client.Config
import org.http4k.serverless.lambda.client.Region

class DeployApiGateway {
    fun deploy() {

        val functionArn = "arn:aws:lambda:us-east-1:145304051762:function:test-function"

        val apis = ListApiResponse.lens(client(Request(GET, "/v2/apis")))
        println(apis)
        apis.items.filter { it.name == "http4k-test-function" }.forEach {
            println("Deleting ${it.apiId}")
            client(Request(DELETE, "/v2/apis/${it.apiId}"))
        }

        ListApiResponse.lens(client(Request(GET, "/v2/apis")))
        val api = ApiInfo.lens(client(Request(POST, "/v2/apis").with(Api.lens of Api("http4k-test-function"))))
        println(api)
        client(Request(POST, "/v2/apis/${api.apiId}/stages").with(Stage.lens of Stage("\$default")))

        val integrationInfo = IntegrationInfo.lens(client(Request(POST, "/v2/apis/${api.apiId}/integrations").with(Integration.lens of Integration(integrationUri = functionArn))))
        println(integrationInfo)

        client(Request(POST, "/v2/apis/${api.apiId}/routes").with(Route.lens of Route("integrations/${integrationInfo.integrationId}")))
    }

    private val config = Environment.ENV overrides Environment.fromResource("/local.properties")
    private val region = Config.region(config)

    private val client = DebuggingFilters.PrintRequestAndResponse()
        .then(ApiGatewayApi(region))
        .then(ClientFilters.AwsAuth(scope(config), Config.credentials(config)))
        .then(JavaHttpClient())

    companion object {
        val scope = EnvironmentKey.map { AwsCredentialScope(it, "apigateway") }.required("region")
    }

}

data class Api(val name: String, val protocolType: String = "HTTP") {
    companion object {
        val lens = Body.auto<Api>().toLens()
    }
}

data class Stage(val stageName: String, val autoDeploy: Boolean = true) {
    companion object {
        val lens = Body.auto<Stage>().toLens()
    }
}

data class ApiInfo(val name: String, val apiId: String, val apiEndpoint: String) {
    companion object {
        val lens = Body.auto<ApiInfo>().toLens()
    }
}

data class ListApiResponse(val items: List<ApiInfo>) {
    companion object {
        val lens = Body.auto<ListApiResponse>().toLens()
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

object ApiGatewayApi {
    operator fun invoke(region: Region): Filter = Filter { next ->
        { request -> next(request.uri(request.uri.host("apigateway.${region.name}.amazonaws.com").scheme("https"))) }
    }
}

fun main() {
    DeployApiGateway().deploy()
}
