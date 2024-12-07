package org.http4k.connect.amazon.apigateway

import org.http4k.connect.amazon.core.model.Region
import org.http4k.core.HttpHandler
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.SetAwsServiceUrl

fun AwsApiGateway.Companion.Http(rawHttp: HttpHandler, region: Region) = object : AwsApiGateway {
    private val http = ClientFilters.SetAwsServiceUrl("apigateway", region.value).then(rawHttp)

    override fun <R : Any> invoke(action: AwsApiGatewayAction<R>) = action.toResult(
        http(action.toRequest())
    )
}
