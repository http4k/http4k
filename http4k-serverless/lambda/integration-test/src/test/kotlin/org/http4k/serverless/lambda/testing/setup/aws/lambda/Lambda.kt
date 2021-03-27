package org.http4k.serverless.lambda.testing.setup.aws.lambda

import com.fasterxml.jackson.annotation.JsonProperty
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.map
import org.http4k.core.HttpHandler
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.DebuggingFilters
import org.http4k.filter.SetAwsServiceUrl
import org.http4k.filter.inIntelliJOnly
import org.http4k.serverless.lambda.testing.setup.aws.RemoteFailure
import org.http4k.serverless.lambda.testing.setup.aws.getOrThrow
import java.nio.ByteBuffer

interface Lambda {
    operator fun <R : Any> invoke(action: LambdaAction<R>): Result<R, RemoteFailure>

    companion object
}

fun Lambda.Companion.Http(rawHttp: HttpHandler, region: Region) = object : Lambda {
    private val http = ClientFilters.SetAwsServiceUrl("lambda", region.name)
        .then(DebuggingFilters.PrintRequestAndResponse().inIntelliJOnly())
        .then(rawHttp)

    override fun <R : Any> invoke(action: LambdaAction<R>) = action.toResult(
        http(action.toRequest())
    )
}

fun Lambda.createFunction(functionPackage: FunctionPackage): FunctionDetails =
    this(CreateFunction(functionPackage)).map { FunctionDetails(it.arn, it.name) }.getOrThrow()

fun Lambda.setPermission(details: FunctionDetails, permission: Permission) =
    this(SetFunctionPermission(details.arn, permission))

fun Lambda.delete(function: Function) = this(DeleteFunction(function)).getOrThrow()

fun Lambda.list() = this(ListFunctions()).getOrThrow()

data class ListFunctionsResponse(@JsonProperty("Functions") val functions: List<FunctionDetailsData>)

data class Permission(
    @JsonProperty("Action") val action: String = "lambda:InvokeFunction",
    @JsonProperty("Principal") val principal: String,
    @JsonProperty("StatementId") val statementId: String
) {
    companion object {
        val invokeFromApiGateway = Permission(principal = "apigateway.amazonaws.com", statementId = "apigateway")
    }
}

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
