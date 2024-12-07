package org.http4k.connect.amazon.lambda

import org.http4k.connect.amazon.core.model.Region
import org.http4k.core.HttpHandler
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.DebuggingFilters
import org.http4k.filter.SetAwsServiceUrl
import org.http4k.filter.inIntelliJOnly

fun Lambda.Companion.Http(rawHttp: HttpHandler, region: Region) = object : Lambda {
    private val http = ClientFilters.SetAwsServiceUrl("lambda", region.value)
        .then(DebuggingFilters.PrintRequestAndResponse().inIntelliJOnly())
        .then(rawHttp)

    override fun <R : Any> invoke(action: LambdaAction<R>) = action.toResult(
        http(action.toRequest())
    )
}
