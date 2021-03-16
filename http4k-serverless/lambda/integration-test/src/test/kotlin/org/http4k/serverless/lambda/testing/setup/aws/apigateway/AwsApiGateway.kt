package org.http4k.serverless.lambda.testing.setup.aws.apigateway

import dev.forkhandles.result4k.Result
import org.http4k.core.HttpHandler
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.SetAwsServiceUrl
import org.http4k.serverless.lambda.testing.setup.Region
import org.http4k.serverless.lambda.testing.setup.aws.RemoteFailure

interface AwsApiGateway {
    operator fun <R : Any> invoke(action: AwsApiGatewayAction<R>): Result<R, RemoteFailure>

    companion object
}

fun AwsApiGateway.Companion.Http(rawHttp: HttpHandler, region: Region) = object : AwsApiGateway {
    private val http = ClientFilters.SetAwsServiceUrl("apigateway", region.name).then(rawHttp)

    override fun <R : Any> invoke(action: AwsApiGatewayAction<R>) = action.toResult(
        http(action.toRequest())
    )
}
