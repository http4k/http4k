package org.http4k.testing.lambda.deployment

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import org.http4k.aws.AwsCredentialScope
import org.http4k.aws.AwsCredentials
import org.http4k.client.JavaHttpClient
import org.http4k.cloudnative.env.Environment
import org.http4k.cloudnative.env.EnvironmentKey
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.core.then
import org.http4k.filter.AwsAuth
import org.http4k.filter.ClientFilters
import org.http4k.filter.DebuggingFilters
import org.junit.jupiter.api.Assumptions.assumeTrue
import java.io.File
import java.nio.ByteBuffer

object DeployServerAsLambdaForClientContract {
    private val config = Environment.ENV overrides Environment.fromResource("/local.properties")
    private val region = Config.region(config)

    private val client = DebuggingFilters.PrintResponse()
        .then(ClientFilters.AwsAuth(Config.scope(config), Config.credentials(config)))
        .then(JavaHttpClient())

    private val deployment = AwsLambdaApiClient(client, region)

    fun deploy() {
        val lambdaBinary = File("http4k-serverless-lambda/test-function/build/libs/test-function-LOCAL-all.jar")

        assumeTrue(lambdaBinary.exists(), "lambda binary to deploy needs to be available")

        val functionName = FunctionName("test-function")

        deployment.delete(functionName)

        val functionPackage = FunctionPackage(
            functionName,
            FunctionHandler("org.http4k.serverless.lambda.LambdaFunction::handle"),
            ByteBuffer.wrap(lambdaBinary.readBytes()),
            Config.role(config),
            mapOf("HTTP4K_BOOTSTRAP_CLASS" to "org.http4k.testing.lambda.TestApp")
        )

        deployment.create(functionPackage)

        assertThat(deployment.list().find { it.name == functionName.value }, present())

        val lambdaClient = LambdaHttpClient(functionName, region).then(client)

        val functionResponse = lambdaClient(Request(Method.GET, "/"))

        assertThat(functionResponse.status, equalTo(Status.OK))
        assertThat(functionResponse.bodyString(), containsSubstring("Hello World"))
    }

    object Config {
        val scope = EnvironmentKey.map { AwsCredentialScope(it, "lambda") }.required("region")
        val region = EnvironmentKey.map(::Region).required("region")
        val role = EnvironmentKey.map(::Role).required("lambdaRuntimeRole")

        fun credentials(config: Environment) =
            AwsCredentials(EnvironmentKey.required("accessKey")(config), EnvironmentKey.required("secretKey")(config))
    }
}

fun main() {
    DeployServerAsLambdaForClientContract.deploy()
}