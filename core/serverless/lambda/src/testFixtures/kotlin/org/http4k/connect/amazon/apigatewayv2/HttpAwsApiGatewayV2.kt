package org.http4k.connect.amazon.apigatewayv2

import org.http4k.connect.amazon.lambda.model.Region
import org.http4k.core.HttpHandler
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.SetAwsServiceUrl

fun AwsApiGatewayV2.Companion.Http(rawHttp: HttpHandler, region: Region) = object : AwsApiGatewayV2 {
    private val http = ClientFilters.SetAwsServiceUrl("apigateway", region.name).then(rawHttp)

    override fun <R : Any> invoke(action: AwsApiGatewayV2Action<R>) = action.toResult(
        http(action.toRequest())
    )
}
