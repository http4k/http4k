package org.http4k.serverless.lambda.testing.setup

import org.http4k.aws.awsCliUserProfiles
import org.http4k.client.JavaHttpClient
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.serverless.lambda.testing.client.awsLambdaApiClient
import org.http4k.serverless.lambda.testing.client.restApiGatewayApiClient
import org.http4k.serverless.lambda.testing.setup.DeployServerAsLambdaForClientContract.functionName
import org.http4k.serverless.lambda.testing.setup.LambdaIntegrationType.ApiGatewayRest
import org.http4k.serverless.lambda.testing.setup.aws.apigatewayv2.ApiName
import org.http4k.serverless.lambda.testing.setup.aws.apigatewayv2.Stage

object DeployRestApiGateway {

    fun deploy() {

        val config = awsCliUserProfiles().profile("http4k-integration-test")

        val functionName = functionName(ApiGatewayRest)

        val lambdaApi = config.awsLambdaApiClient()

        val functionArn = lambdaApi.list().find { it.name == functionName.value }?.arn
            ?: error("Lambda ${functionName.value} does not exist.")

        val apiGateway = config.restApiGatewayApiClient()

        val apis = apiGateway.listApis()

        apis.filter { it.name == apiName() }.forEach {
            println("Deleting ${it.apiId.value}")
            apiGateway.delete(it.apiId)
        }

        val api = apiGateway.createApi(apiName())
        apiGateway.createStage(api.apiId, Stage.restDefault, functionArn)

        waitUntil(OK) {
            JavaHttpClient()(Request(GET, api.apiEndpoint.path("/default/empty"))).also { println(it.status) }
        }
    }

    fun apiName() = ApiName("http4k-rest-api")
}

fun main() {
    DeployRestApiGateway.deploy()
}
