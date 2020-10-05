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
import org.http4k.serverless.lambda.client.ApiName
import org.http4k.serverless.lambda.client.AwsApiGatewayApiClient
import org.http4k.serverless.lambda.client.Config
import org.http4k.serverless.lambda.client.Route
import org.http4k.serverless.lambda.client.Stage

class DeployApiGateway {
    fun deploy() {
        val apiGateway = AwsApiGatewayApiClient(client, region)

        val functionArn = "arn:aws:lambda:us-east-1:145304051762:function:test-function"
        val apiName = ApiName("http4k-test-function")

        val apis = apiGateway.listApis()

        apis.filter { it.name == apiName }.forEach {
            println("Deleting ${it.apiId.value}")
            apiGateway.delete(it.apiId)
        }

        println(apiGateway.listApis())

        val api = apiGateway.createApi(apiName)
        apiGateway.createStage(api.apiId, Stage.default)

        val integrationInfo = apiGateway.createLambdaIntegration(api.apiId, functionArn)
        println(integrationInfo)

        client(Request(POST, "/v2/apis/${api.apiId.value}/routes").with(Route.lens of Route("integrations/${integrationInfo.value}")))

        //TODO add a call (with retries + timeout) to the deployed api
        // println(JavaHttpClient()(Request(Method.GET, api.apiEndpoint.path("/empty"))))
        println("Created API: $api")
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
