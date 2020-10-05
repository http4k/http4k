package org.http4k.serverless.lambda

import org.http4k.aws.AwsCredentialScope
import org.http4k.client.JavaHttpClient
import org.http4k.cloudnative.env.Environment
import org.http4k.cloudnative.env.EnvironmentKey
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.AwsAuth
import org.http4k.filter.ClientFilters
import org.http4k.filter.DebuggingFilters
import org.http4k.serverless.lambda.client.AwsApiGatewayApiClient
import org.http4k.serverless.lambda.client.Config
import org.http4k.serverless.lambda.client.Integration
import org.http4k.serverless.lambda.client.IntegrationInfo
import org.http4k.serverless.lambda.client.Route
import org.http4k.serverless.lambda.client.Stage

class DeployApiGateway {
    fun deploy() {
        val apiGateway = AwsApiGatewayApiClient(client, region)

        val functionArn = "arn:aws:lambda:us-east-1:145304051762:function:test-function"

        val apis = apiGateway.listApis()

        println(apis)
        apis.filter { it.name == "http4k-test-function" }.forEach {
            println("Deleting ${it.apiId}")
            apiGateway.delete(it.apiId)
        }

        println(apiGateway.listApis())

        val api = apiGateway.create("http4k-test-function")
        println(api)
        client(Request(POST, "/v2/apis/${api.apiId}/stages").with(Stage.lens of Stage("\$default")))

        val integrationInfo = IntegrationInfo.lens(client(Request(POST, "/v2/apis/${api.apiId}/integrations").with(Integration.lens of Integration(integrationUri = functionArn))))
        println(integrationInfo)

        client(Request(POST, "/v2/apis/${api.apiId}/routes").with(Route.lens of Route("integrations/${integrationInfo.integrationId}")))
    }

    private val config = Environment.ENV overrides Environment.fromResource("/local.properties")
    private val region = Config.region(config)

    private val client = DebuggingFilters.PrintRequestAndResponse()
        .then(AwsApiGatewayApiClient.ApiGatewayApi(region)) //TODO delete once all calls are moved into client
        .then(ClientFilters.AwsAuth(scope(config), Config.credentials(config)))
        .then(JavaHttpClient())

    companion object {
        val scope = EnvironmentKey.map { AwsCredentialScope(it, "apigateway") }.required("region")
    }

}

fun main() {
    DeployApiGateway().deploy()
}
