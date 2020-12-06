package org.http4k.client

import org.http4k.aws.Function
import org.http4k.aws.Region
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ClientFilters
import org.http4k.filter.SetAwsServiceUrl

abstract class LambdaHttpClient(function: Function, region: Region) : Filter {
    private fun createFunctionRequest(function: Function) = Filter { next ->
        {
            extract(
                next(
                    Request(POST, "/2015-03-31/functions/${function.value}/invocations")
                        .header("X-Amz-Invocation-Type", "RequestResponse")
                        .header("X-Amz-Log-Type", "Tail")
                        .with(inject(it)))
            )
        }
    }

    protected abstract fun inject(it: Request): (Request) -> Request

    protected abstract fun extract(lambdaResponse: Response): Response

    private val filter = createFunctionRequest(function)
        .then(ClientFilters.SetAwsServiceUrl("lambda", region.name))

    override fun invoke(handler: HttpHandler): HttpHandler = filter(handler)
}
