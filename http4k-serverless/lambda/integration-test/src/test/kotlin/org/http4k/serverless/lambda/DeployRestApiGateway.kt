package org.http4k.serverless.lambda

import org.http4k.aws.ApiIntegrationVersion
import org.http4k.aws.ApiIntegrationVersion.v1
import org.http4k.aws.ApiIntegrationVersion.v2
import org.http4k.aws.ApiName
import org.http4k.aws.LambdaIntegrationType
import org.http4k.aws.LambdaIntegrationType.ApiGatewayV1
import org.http4k.aws.LambdaIntegrationType.ApiGatewayV2
import org.http4k.aws.Stage
import org.http4k.aws.awsCliUserProfiles
import org.http4k.client.JavaHttpClient
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.serverless.lambda.DeployServerAsLambdaForClientContract.functionName
import org.http4k.serverless.lambda.client.awsLambdaApiClient
import org.http4k.serverless.lambda.client.restApiGatewayApiClient

object DeployRestApiGateway {

    fun deploy(integrationVersion: ApiIntegrationVersion) {

        val config = awsCliUserProfiles().profile("http4k-integration-test")

        val functionName = functionName(integrationVersion.integrationType())

        val lambdaApi = config.awsLambdaApiClient()

        val functionArn = lambdaApi.list().find { it.name == functionName.value }?.arn
            ?: error("Lambda ${functionName.value} does not exist.")

        val apiGateway = config.restApiGatewayApiClient()

        val apis = apiGateway.listApis()

        apis.filter { it.name == apiName(integrationVersion) }.forEach {
            println("Deleting ${it.apiId.value}")
            apiGateway.delete(it.apiId)
        }

        val api = apiGateway.createApi(apiName(integrationVersion))
        apiGateway.createStage(api.apiId, Stage.restDefault, functionArn)

        val integrationId = apiGateway.createLambdaIntegration(api.apiId, functionArn, integrationVersion)

        apiGateway.createDefaultRoute(api.apiId, integrationId)

        waitUntil(OK) {
            JavaHttpClient()(Request(GET, api.apiEndpoint.path("/default/empty"))).also { println(it.status) }
        }
    }

    fun apiName(version: ApiIntegrationVersion) = ApiName("http4k-rest-api-${version.name}")

    private fun ApiIntegrationVersion.integrationType(): LambdaIntegrationType = when (this) {
        v1 -> ApiGatewayV1
        v2 -> ApiGatewayV2
    }
}

fun main() {
    DeployRestApiGateway.deploy(v1)
}
