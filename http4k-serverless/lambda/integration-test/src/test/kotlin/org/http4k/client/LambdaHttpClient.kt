package org.http4k.client

import org.http4k.aws.FunctionName
import org.http4k.aws.Region
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.then
import org.http4k.core.with

abstract class LambdaHttpClient(functionName: FunctionName, region: Region) : Filter {
    private fun callFunction(functionName: FunctionName) = Filter { next ->
        {
            extract(next(Request(POST, "/2015-03-31/functions/${functionName.value}/invocations")
                .header("X-Amz-Invocation-Type", "RequestResponse")
                .header("X-Amz-Log-Type", "Tail")
                .with(inject(it))))
        }
    }

    protected abstract fun inject(it: Request): (Request) -> Request

    protected abstract fun extract(lambdaResponse: Response): Response

    private val filter = callFunction(functionName).then(LambdaApi(region))

    override fun invoke(handler: HttpHandler): HttpHandler = filter(handler)
}
