package org.http4k.serverless.lambda

import org.http4k.aws.AwsCredentialScope
import org.http4k.client.JavaHttpClient
import org.http4k.cloudnative.env.Environment
import org.http4k.cloudnative.env.EnvironmentKey
import org.http4k.core.then
import org.http4k.filter.AwsAuth
import org.http4k.filter.ClientFilters
import org.http4k.filter.DebuggingFilters
import org.http4k.serverless.lambda.client.ApiName
import org.http4k.serverless.lambda.client.AwsApiGatewayApiClient
import org.http4k.serverless.lambda.client.Config
import org.http4k.serverless.lambda.client.Stage

class DeployApiGateway {
    private val config = Environment.ENV overrides Environment.fromResource("/local.properties")
    private val region = Config.region(config)

    private val client = DebuggingFilters.PrintRequestAndResponse()
        .then(AwsApiGatewayApiClient.ApiGatewayApi(region)) //TODO delete once all calls are moved into client
        .then(ClientFilters.AwsAuth(scope(config), Config.credentials(config)))
        .then(JavaHttpClient())

    fun deploy() {
        val apiGateway = AwsApiGatewayApiClient(client, region)

        val functionArn = "arn:aws:lambda:us-east-1:145304051762:function:test-function"
        val apiName = ApiName("http4k-test-function")

        val apis = apiGateway.listApis()

        apis.filter { it.name == apiName }.forEach {
            println("Deleting ${it.apiId.value}")
            apiGateway.delete(it.apiId)
        }

        val api = apiGateway.createApi(apiName)
        apiGateway.createStage(api.apiId, Stage.default)

        val integrationId = apiGateway.createLambdaIntegration(api.apiId, functionArn)

        apiGateway.createDefaultRoute(api.apiId, integrationId)

        //TODO add a call (with retries + timeout) to the deployed api
        // println(JavaHttpClient()(Request(Method.GET, api.apiEndpoint.path("/empty"))))
        println("Created API: $api")
    }

    companion object {
        val scope = EnvironmentKey.map { AwsCredentialScope(it, "apigateway") }.required("region")
    }
}

fun main() {
    DeployApiGateway().deploy()
}
