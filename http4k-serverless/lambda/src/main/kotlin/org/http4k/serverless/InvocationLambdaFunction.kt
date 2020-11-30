package org.http4k.serverless

import com.amazonaws.services.lambda.runtime.Context
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response

abstract class InvocationLambdaFunction(appLoader: AppLoaderWithContexts)
    : AwsLambdaFunction<String, String>(InvocationLambdaAwsHttpAdapter, appLoader) {
    constructor(input: AppLoader) : this(AppLoaderWithContexts { env, _ -> input(env) })
    constructor(input: HttpHandler) : this(AppLoader { input })

    override fun handleRequest(req: String, ctx: Context) = handle(req, ctx)
}

internal object InvocationLambdaAwsHttpAdapter : AwsHttpAdapter<String, String> {
    override fun invoke(req: String, ctx: Context) =
        Request(POST, "/2015-03-31/functions/${ctx.functionName}/invocations")
            .header("X-Amz-Invocation-Type", "RequestResponse")
            .header("X-Amz-Log-Type", "Tail").body(req)

    override fun invoke(req: Response) = req.bodyString()
}

