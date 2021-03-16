package org.http4k.serverless.lambda.testing.setup.aws.lambda

import com.fasterxml.jackson.annotation.JsonProperty
import dev.forkhandles.result4k.map
import org.http4k.core.HttpHandler
import org.http4k.serverless.lambda.testing.setup.aws.getOrThrow
import java.nio.ByteBuffer


class AwsLambdaApiClient(client: HttpHandler, region: Region) {
    private val lambda = Lambda.Http(client, region)

    fun create(functionPackage: FunctionPackage): FunctionDetails {
        val details = lambda(CreateFunction(functionPackage)).map { FunctionDetails(it.arn, it.name) }.getOrThrow()
        lambda(SetFunctionPermission(details.arn, Permission.invokeFromApiGateway))
        return details
    }

    fun delete(function: Function) = lambda(DeleteFunction(function)).getOrThrow()

    fun list() = lambda(ListFunctions()).map { it.functions.map { f -> FunctionDetails(f.arn, f.name) } }.getOrThrow()
}

data class ListFunctionsResponse(@JsonProperty("Functions") val functions: List<FunctionDetailsData>)

data class FunctionDetailsData(
    @JsonProperty("FunctionArn") val arn: String,
    @JsonProperty("FunctionName") val name: String
)

data class FunctionPackage(
    val name: Function,
    val handler: FunctionHandler,
    val jar: ByteBuffer,
    val role: Role,
    val environmentProperties: Map<String, String> = mapOf(),
    val timeoutInSeconds: Int = 15
)

data class Function(val value: String)

data class FunctionHandler(val value: String)

data class FunctionDetails(val arn: String, val name: String)

data class Role(val name: String)

data class Region(val name: String)

enum class LambdaIntegrationType { ApiGatewayRest, ApiGatewayV1, ApiGatewayV2, ApplicationLoadBalancer, Invocation }
