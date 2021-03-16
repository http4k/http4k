package org.http4k.serverless.lambda.testing.setup

import org.http4k.aws.awsCliUserProfiles
import org.http4k.client.JavaHttpClient
import org.http4k.cloudnative.env.Timeout
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.OK
import org.http4k.serverless.lambda.testing.client.apiGatewayApiClient
import org.http4k.serverless.lambda.testing.client.awsLambdaApiClient
import org.http4k.serverless.lambda.testing.setup.DeployServerAsLambdaForClientContract.functionName
import org.http4k.serverless.lambda.testing.setup.aws.apigatewayv2.ApiIntegrationVersion
import org.http4k.serverless.lambda.testing.setup.aws.apigatewayv2.ApiIntegrationVersion.v1
import org.http4k.serverless.lambda.testing.setup.aws.apigatewayv2.ApiIntegrationVersion.v2
import org.http4k.serverless.lambda.testing.setup.aws.apigatewayv2.ApiName
import org.http4k.serverless.lambda.testing.setup.aws.apigatewayv2.Stage
import org.http4k.serverless.lambda.testing.setup.aws.apigatewayv2.createApi
import org.http4k.serverless.lambda.testing.setup.aws.apigatewayv2.createDefaultRoute
import org.http4k.serverless.lambda.testing.setup.aws.apigatewayv2.createLambdaIntegration
import org.http4k.serverless.lambda.testing.setup.aws.apigatewayv2.createStage
import org.http4k.serverless.lambda.testing.setup.aws.apigatewayv2.delete
import org.http4k.serverless.lambda.testing.setup.aws.apigatewayv2.listApis
import org.http4k.serverless.lambda.testing.setup.aws.lambda.LambdaIntegrationType
import org.http4k.serverless.lambda.testing.setup.aws.lambda.LambdaIntegrationType.ApiGatewayV1
import org.http4k.serverless.lambda.testing.setup.aws.lambda.LambdaIntegrationType.ApiGatewayV2
import org.http4k.serverless.lambda.testing.setup.aws.lambda.list
import org.junit.jupiter.api.fail
import java.time.Duration
import java.time.Instant

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

        waitUntil(OK) {
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
    ApiIntegrationVersion.values().forEach(DeployHttpApiGateway::deploy)
}

fun waitUntil(
    status: Status,
    timeout: Timeout = Timeout(Duration.ofSeconds(5)),
    retryEvery: Duration = Duration.ofMillis(500),
    action: () -> Response
) {
    val start = Instant.now()
    var success: Boolean
    do {
        success = action().status == status
        if (!success) {
            if (Duration.ofMillis(Instant.now().toEpochMilli() - start.toEpochMilli()) > timeout.value) {
                fail("Timed out after ${timeout.value}")
            }
            Thread.sleep(retryEvery.toMillis())
        }
    } while (!success)
}
