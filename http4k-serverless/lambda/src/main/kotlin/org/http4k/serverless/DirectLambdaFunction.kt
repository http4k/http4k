package org.http4k.serverless

import com.amazonaws.services.lambda.runtime.Context
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response

abstract class DirectLambdaFunction(appLoader: AppLoaderWithContexts)
    : AwsLambdaFunction<String, String>(DirectLambdaAwsHttpAdapter, appLoader) {
    constructor(input: AppLoader) : this(AppLoaderWithContexts { env, _ -> input(env) })
    constructor(input: HttpHandler) : this(AppLoader { input })

    override fun handleRequest(req: String, ctx: Context) = handle(req, ctx)
}

internal object DirectLambdaAwsHttpAdapter : AwsHttpAdapter<String, String> {
    override fun invoke(req: String) = Request(POST, "").body(req)

    override fun invoke(req: Response) = req.bodyString()
}

