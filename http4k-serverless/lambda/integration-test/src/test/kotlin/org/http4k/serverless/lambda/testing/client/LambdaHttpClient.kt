package org.http4k.serverless.lambda.testing.client

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ClientFilters
import org.http4k.filter.SetAwsServiceUrl
import org.http4k.serverless.lambda.testing.setup.aws.lambda.Function
import org.http4k.serverless.lambda.testing.setup.aws.lambda.Region

abstract class LambdaHttpClient(function: Function, region: Region) : Filter {
    private fun createFunctionRequest(function: Function) = Filter { next ->
        {
            next(
                Request(POST, "/2015-03-31/functions/${function.value}/invocations")
                    .header("X-Amz-Invocation-Type", "RequestResponse")
                    .header("X-Amz-Log-Type", "Tail")
                    .with(it.toLambdaFormat())).fromLambdaFormat()
        }
    }

    protected abstract fun Request.toLambdaFormat(): (Request) -> Request

    protected abstract fun Response.fromLambdaFormat(): Response

    private val filter = createFunctionRequest(function)
        .then(ClientFilters.SetAwsServiceUrl("lambda", region.name))

    override fun invoke(handler: HttpHandler): HttpHandler = filter(handler)
}
