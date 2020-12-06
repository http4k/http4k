@file:Suppress("EnumEntryName")

package org.http4k.serverless.lambda

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import org.http4k.aws.AwsLambdaApiClient
import org.http4k.aws.Function
import org.http4k.aws.FunctionHandler
import org.http4k.aws.FunctionPackage
import org.http4k.aws.LambdaIntegrationType
import org.http4k.aws.LambdaIntegrationType.ApiGatewayV1
import org.http4k.aws.LambdaIntegrationType.ApiGatewayV2
import org.http4k.aws.LambdaIntegrationType.ApplicationLoadBalancer
import org.http4k.aws.LambdaIntegrationType.Invocation
import org.http4k.aws.Region
import org.http4k.aws.Role
import org.http4k.aws.awsCliUserProfiles
import org.http4k.client.ApiGatewayV1LambdaClient
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.serverless.lambda.client.awsClientFor
import org.http4k.serverless.lambda.client.testFunctionClient
import org.junit.jupiter.api.Assumptions.assumeTrue
import java.io.File
import java.nio.ByteBuffer

object DeployServerAsLambdaForClientContract {

    fun deploy(version: LambdaIntegrationType) {
        val functionName = functionName(version)

        val config = awsCliUserProfiles().profile("default")

        val lambdaBinary =
            File("test-function/build/distributions/test-function-LOCAL.zip")

        assumeTrue(lambdaBinary.exists(), "lambda binary to deploy (${lambdaBinary.absolutePath}) needs to be available")

        val lambdaApiClient = AwsLambdaApiClient(config.awsClientFor("lambda"), Region(config.region))

        println("Deleting existing function (if exists)...")
        lambdaApiClient.delete(functionName)

        val functionPackage = FunctionPackage(
            functionName,
            FunctionHandler(version.functionMainClass()),
            ByteBuffer.wrap(lambdaBinary.readBytes()),
            Role(config["lambda_runtime_role"])
        )

        println("Deploying function...")
        val details = lambdaApiClient.create(functionPackage)

        println("Created function with arn ${details.arn}")

        assertThat(lambdaApiClient.list().find { it.name == functionName.value }, present())

        println("Performing a test request...")
        val client = config.testFunctionClient(ApiGatewayV1, ::ApiGatewayV1LambdaClient)
        val functionResponse = client(Request(Method.POST, "/echo").body("Hello, http4k"))

        assertThat(functionResponse.status, equalTo(Status.OK))
        assertThat(functionResponse.bodyString(), containsSubstring("Hello, http4k"))
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
    LambdaIntegrationType.values().forEach(DeployServerAsLambdaForClientContract::deploy)
}

