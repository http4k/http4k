@file:Suppress("EnumEntryName")

package org.http4k.serverless.lambda

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import org.http4k.aws.FunctionHandler
import org.http4k.aws.FunctionName
import org.http4k.aws.FunctionPackage
import org.http4k.aws.ApiIntegrationVersion
import org.http4k.aws.ApiIntegrationVersion.v1
import org.http4k.aws.ApiIntegrationVersion.v2
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.serverless.lambda.client.Config
import org.http4k.serverless.lambda.client.awsConfig
import org.http4k.serverless.lambda.client.lambdaApiClient
import org.http4k.serverless.lambda.client.testFunctionClient
import org.junit.jupiter.api.Assumptions.assumeTrue
import java.io.File
import java.nio.ByteBuffer

object DeployServerAsLambdaForClientContract {

    fun deploy(version: ApiIntegrationVersion) {
        val functionName = functionName(version)

        val lambdaBinary =
            File("http4k-serverless-lambda/integration-test/test-function/build/distributions/test-function-LOCAL.zip")

        assumeTrue(lambdaBinary.exists(), "lambda binary to deploy (${lambdaBinary.absolutePath}) needs to be available")

        println("Deleting existing function (if exists)...")
        lambdaApiClient.delete(functionName)

        val functionPackage = FunctionPackage(
            functionName,
            FunctionHandler(functionMainClass(version)),
            ByteBuffer.wrap(lambdaBinary.readBytes()),
            Config.role(awsConfig)
        )

        println("Deploying function...")
        val details = lambdaApiClient.create(functionPackage)

        println("Created function with arn ${details.arn}")

        assertThat(lambdaApiClient.list().find { it.name == functionName.value }, present())

        println("Performing a test request...")
        val functionResponse = testFunctionClient(Request(Method.POST, "/echo").body("Hello, http4k"))

        assertThat(functionResponse.status, equalTo(Status.OK))
        assertThat(functionResponse.bodyString(), containsSubstring("Hello, http4k"))
    }

    fun functionName(version: ApiIntegrationVersion) = FunctionName("test-function-${version.name}")

    private fun functionMainClass(version: ApiIntegrationVersion): String = when (version) {
        v1 -> "org.http4k.serverless.lambda.TestFunctionV1"
        v2 -> "org.http4k.serverless.lambda.TestFunctionV2"
    }
}

fun main() {
    DeployServerAsLambdaForClientContract.deploy(v1)
    DeployServerAsLambdaForClientContract.deploy(v2)
}
