package org.http4k.serverless.lambda

import org.http4k.aws.ApiIntegrationVersion
import org.http4k.aws.ApiIntegrationVersion.v1
import org.http4k.aws.ApiIntegrationVersion.v2
import org.http4k.aws.ApiName
import org.http4k.aws.LambdaIntegrationType
import org.http4k.aws.LambdaIntegrationType.ApiGatewayV1
import org.http4k.aws.LambdaIntegrationType.ApiGatewayV2
import org.http4k.aws.Stage
import org.http4k.client.JavaHttpClient
import org.http4k.cloudnative.env.Timeout
import org.http4k.core.Filter
import org.http4k.core.Method.GET
import org.http4k.core.NoOp
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.OK
import org.http4k.serverless.lambda.DeployServerAsLambdaForClientContract.functionName
import org.http4k.serverless.lambda.client.apiGatewayClient
import org.http4k.serverless.lambda.client.lambdaApiClient
import org.junit.jupiter.api.fail
import java.lang.management.ManagementFactory
import java.time.Duration
import java.time.Instant

object DeployApiGateway {

    fun deploy(version: ApiIntegrationVersion) {
        val functionName = functionName(version.integrationType())
        val apiName = apiName(version)

        val functionArn = lambdaApiClient.list().find { it.name == functionName.value }?.arn
            ?: error("Lambda ${functionName.value} does not exist.")

        val apis = apiGatewayClient.listApis()

        apis.filter { it.name == apiName }.forEach {
            println("Deleting ${it.apiId.value}")
            apiGatewayClient.delete(it.apiId)
        }

        val api = apiGatewayClient.createApi(apiName)
        apiGatewayClient.createStage(api.apiId, Stage.default)

        val integrationId = apiGatewayClient.createLambdaIntegration(api.apiId, functionArn, version)

        apiGatewayClient.createDefaultRoute(api.apiId, integrationId)

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
    ApiIntegrationVersion.values().forEach(DeployApiGateway::deploy)
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

fun inIntelliJOnly(filter: Filter) =
    if (ManagementFactory.getRuntimeMXBean().inputArguments.find { it.contains("idea", true) } != null)
        filter
    else Filter.NoOp
