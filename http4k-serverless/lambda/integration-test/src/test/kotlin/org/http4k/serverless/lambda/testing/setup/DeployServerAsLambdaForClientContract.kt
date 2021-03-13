@file:Suppress("EnumEntryName")

package org.http4k.serverless.lambda.testing.setup

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import org.http4k.aws.awsCliUserProfiles
import org.http4k.aws.awsClientFor
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookie
import org.http4k.core.then
import org.http4k.serverless.lambda.testing.client.ApiGatewayV1LambdaClient
import org.http4k.serverless.lambda.testing.client.ApiGatewayV2LambdaClient
import org.http4k.serverless.lambda.testing.client.ApplicationLoadBalancerLambdaClient
import org.http4k.serverless.lambda.testing.client.InvocationLambdaClient
import org.http4k.serverless.lambda.testing.client.LambdaHttpClient
import org.http4k.serverless.lambda.testing.client.awsLambdaApiClient
import org.http4k.serverless.lambda.testing.setup.LambdaIntegrationType.ApiGatewayV1
import org.http4k.serverless.lambda.testing.setup.LambdaIntegrationType.ApiGatewayV2
import org.http4k.serverless.lambda.testing.setup.LambdaIntegrationType.ApplicationLoadBalancer
import org.http4k.serverless.lambda.testing.setup.LambdaIntegrationType.Invocation
import org.junit.jupiter.api.Assumptions.assumeTrue
import java.io.File
import java.io.PrintStream
import java.nio.ByteBuffer

object DeployServerAsLambdaForClientContract {

    fun deploy(type: LambdaIntegrationType, clientFn: (Function, Region) -> LambdaHttpClient) {
        val functionName = functionName(type)

        val config = awsCliUserProfiles().profile("http4k-integration-test")

        val lambdaBinary =
            File("test-function/build/distributions/test-function-LOCAL.zip")

        assumeTrue(lambdaBinary.exists(), "lambda binary to deploy (${lambdaBinary.absolutePath}) needs to be available")

        val lambdaApiClient = config.awsLambdaApiClient()

        println("Deleting existing function (if exists)...")
        lambdaApiClient.delete(functionName)

        val functionPackage = FunctionPackage(
            functionName,
            FunctionHandler(type.functionMainClass()),
            ByteBuffer.wrap(lambdaBinary.readBytes()),
            Role(config["lambda_runtime_role"])
        )

        println("Deploying function...")
        val details = lambdaApiClient.create(functionPackage)

        println("Created function with arn ${details.arn}")

        assertThat(lambdaApiClient.list().find { it.name == functionName.value }, present())

        println("Performing a test request...")
        val client = clientFn(functionName(type), Region(config.region))
            .then(config.awsClientFor("lambda").debugBodies())
        val functionResponse = client(Request(POST, "/")
            .query("query1", "queryValue1")
            .query("query1", "queryValue2")
            .query("query2", "queryValue3")
            .header("single", "value1")
            .header("multi", "value2")
            .header("multi", "value3")
            .cookie(Cookie("cookie1", "value1"))
            .cookie(Cookie("cookie2", "value2"))
            .body("""{"hello":"http4k"}"""))

        assertThat(functionResponse.status, equalTo(OK))
        assertThat(functionResponse.bodyString(), containsSubstring("""{"hello":"http4k"}"""))
    }

    fun functionName(version: LambdaIntegrationType) = Function("test-function-${version.functionNamePrefix()}")

    private fun LambdaIntegrationType.functionMainClass(): String = when (this) {
        ApiGatewayV1 -> "org.http4k.serverless.lambda.TestFunctionV1"
        ApiGatewayV2 -> "org.http4k.serverless.lambda.TestFunctionV2"
        ApplicationLoadBalancer -> "org.http4k.serverless.lambda.TestFunctionAlb"
        Invocation -> "org.http4k.serverless.lambda.TestFunctionInvocation"
    }

    private fun LambdaIntegrationType.functionNamePrefix(): String = when (this) {
        ApiGatewayV1 -> "apigw-v1"
        ApiGatewayV2 -> "apigw-v2"
        ApplicationLoadBalancer -> "alb"
        Invocation -> "invoke"
    }
}

fun main() {
    DeployServerAsLambdaForClientContract.apply {
        deploy(ApiGatewayV1, ::ApiGatewayV1LambdaClient)
        deploy(ApiGatewayV2, ::ApiGatewayV2LambdaClient)
        deploy(ApplicationLoadBalancer, ::ApplicationLoadBalancerLambdaClient)
        deploy(Invocation, ::InvocationLambdaClient)
    }
}

private fun HttpHandler.debugBodies(out: PrintStream = System.out) = Filter { next ->
    {
        out.println("******** REQUEST ${it.method} ${it.uri}\n" + it.bodyString())
        next(it).also { r -> out.println("******** RESPONSE ${it.method} ${it.uri}\n" + r.bodyString()) }
    }
}.then(this)
