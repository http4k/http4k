package org.http4k.connect.amazon.lambda

import com.fasterxml.jackson.annotation.JsonProperty
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.map
import org.http4k.core.HttpHandler
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.DebuggingFilters
import org.http4k.filter.SetAwsServiceUrl
import org.http4k.filter.inIntelliJOnly
import org.http4k.connect.amazon.RemoteFailure
import org.http4k.connect.amazon.getOrThrow
import org.http4k.connect.amazon.lambda.action.CreateFunction
import org.http4k.connect.amazon.lambda.action.DeleteFunction
import org.http4k.connect.amazon.lambda.action.ListFunctions
import org.http4k.connect.amazon.lambda.action.SetFunctionPermission
import org.http4k.connect.amazon.lambda.model.Function
import org.http4k.connect.amazon.lambda.model.FunctionDetails
import org.http4k.connect.amazon.lambda.model.FunctionPackage
import org.http4k.connect.amazon.lambda.model.Region

interface Lambda {
    operator fun <R : Any> invoke(action: LambdaAction<R>): Result<R, RemoteFailure>

    companion object
}



fun Lambda.createFunction(functionPackage: FunctionPackage): FunctionDetails =
    this(CreateFunction(functionPackage)).map { FunctionDetails(it.arn, it.name) }.getOrThrow()

fun Lambda.setPermission(details: FunctionDetails, permission: Permission) =
    this(SetFunctionPermission(details.arn, permission))

fun Lambda.delete(function: Function) = this(DeleteFunction(function)).getOrThrow()

fun Lambda.list() = this(ListFunctions()).getOrThrow()

data class ListFunctionsResponse(@JsonProperty("Functions") val functions: List<FunctionDetailsData>)

data class FunctionDetailsData(
    @JsonProperty("FunctionArn") val arn: String,
    @JsonProperty("FunctionName") val name: String
)

data class Permission(
    @JsonProperty("Action") val action: String = "lambda:InvokeFunction",
    @JsonProperty("Principal") val principal: String,
    @JsonProperty("StatementId") val statementId: String
) {
    companion object {
        val invokeFromApiGateway = Permission(principal = "apigateway.amazonaws.com", statementId = "apigateway")
    }
}
