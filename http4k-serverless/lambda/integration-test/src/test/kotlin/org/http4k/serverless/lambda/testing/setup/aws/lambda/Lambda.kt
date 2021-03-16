package org.http4k.serverless.lambda.testing.setup.aws.lambda

import com.fasterxml.jackson.annotation.JsonProperty
import dev.forkhandles.result4k.Result
import org.http4k.core.HttpHandler
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.DebuggingFilters
import org.http4k.filter.SetAwsServiceUrl
import org.http4k.filter.inIntelliJOnly
import org.http4k.serverless.lambda.testing.setup.aws.RemoteFailure

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

data class Permission(
    @JsonProperty("Action") val action: String = "lambda:InvokeFunction",
    @JsonProperty("Principal") val principal: String,
    @JsonProperty("StatementId") val statementId: String
) {
    companion object {
        val invokeFromApiGateway = Permission(principal = "apigateway.amazonaws.com", statementId = "apigateway")
    }
}
