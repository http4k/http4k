package org.http4k.serverless.lambda.testing.setup

import org.http4k.aws.awsCliUserProfiles
import org.http4k.client.JavaHttpClient
import org.http4k.connect.amazon.apigatewayv2.action.createApi
import org.http4k.connect.amazon.apigatewayv2.action.createDefaultRoute
import org.http4k.connect.amazon.apigatewayv2.action.createLambdaIntegration
import org.http4k.connect.amazon.apigatewayv2.action.createStage
import org.http4k.connect.amazon.apigatewayv2.action.delete
import org.http4k.connect.amazon.apigatewayv2.action.listApis
import org.http4k.connect.amazon.apigatewayv2.model.ApiIntegrationVersion
import org.http4k.connect.amazon.apigatewayv2.model.ApiIntegrationVersion.v1
import org.http4k.connect.amazon.apigatewayv2.model.ApiIntegrationVersion.v2
import org.http4k.connect.amazon.apigatewayv2.model.ApiName
import org.http4k.connect.amazon.apigatewayv2.model.Stage
import org.http4k.connect.amazon.lambda.action.list
import org.http4k.connect.amazon.lambda.model.LambdaIntegrationType
import org.http4k.connect.amazon.lambda.model.LambdaIntegrationType.ApiGatewayV1
import org.http4k.connect.amazon.lambda.model.LambdaIntegrationType.ApiGatewayV2
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.serverless.lambda.testing.client.apiGatewayApiClient
import org.http4k.serverless.lambda.testing.client.awsLambdaApiClient
import org.http4k.serverless.lambda.testing.setup.DeployServerAsLambdaForClientContract.functionName

object DeployHttpApiGateway {

    fun deploy(integrationVersion: ApiIntegrationVersion) {

        val config = awsCliUserProfiles().profile("http4k-integration-test")

        val functionName = functionName(integrationVersion.integrationType())

        val lambdaApi = config.awsLambdaApiClient()

        val functionArn = lambdaApi.list().find { it.name == functionName.value }?.arn
            ?: error("Lambda ${functionName.value} does not exist.")

        val apiGateway = config.apiGatewayApiClient()

        val apis = apiGateway.listApis()

        apis.filter { it.name == apiName(integrationVersion) }.forEach {
            println("Deleting ${it.apiId.value}")
            apiGateway.delete(it.apiId)
        }

        val api = apiGateway.createApi(apiName(integrationVersion))
        apiGateway.createStage(api.apiId, Stage.default)

        val integrationId = apiGateway.createLambdaIntegration(api.apiId, functionArn, integrationVersion)

        apiGateway.createDefaultRoute(api.apiId, integrationId)

        retryUntil(OK) {
            JavaHttpClient()(Request(GET, api.apiEndpoint.path("/empty"))).also { println(it.status) }
        }
    }

    fun apiName(version: ApiIntegrationVersion) = ApiName("http4k-test-function-${version.name}")

    private fun ApiIntegrationVersion.integrationType(): LambdaIntegrationType = when (this) {
        v1 -> ApiGatewayV1
        v2 -> ApiGatewayV2
    }
}

fun main() {
    ApiIntegrationVersion.entries.forEach(DeployHttpApiGateway::deploy)
}

