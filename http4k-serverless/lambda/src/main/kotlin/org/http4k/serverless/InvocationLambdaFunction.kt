package org.http4k.serverless

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestStreamHandler
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import java.io.InputStream
import java.io.OutputStream

/**
 * This is the main entry point for lambda invocations using the direct invocations.
 * It uses the local environment to instantiate the HttpHandler which can be used
 * for further invocations.
 */
abstract class InvocationLambdaFunction(appLoader: AppLoaderWithContexts)
    : AwsLambdaFunction<InputStream, InputStream>(InvocationLambdaAwsHttpAdapter, appLoader), RequestStreamHandler {
    constructor(input: AppLoader) : this(AppLoaderWithContexts { env, _ -> input(env) })
    constructor(input: HttpHandler) : this(AppLoader { input })

    override fun handleRequest(input: InputStream, output: OutputStream, context: Context) {
        handle(input, context).copyTo(output)
    }
}

object InvocationLambdaAwsHttpAdapter : AwsHttpAdapter<InputStream, InputStream> {
    override fun invoke(req: InputStream, ctx: Context) =
        Request(POST, "/2015-03-31/functions/${ctx.functionName}/invocations")
            .header("X-Amz-Invocation-Type", "RequestResponse")
            .header("X-Amz-Log-Type", "Tail").body(req)

    override fun invoke(req: Response) = req.body.stream
}

