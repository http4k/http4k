package org.http4k.serverless

import com.amazonaws.services.lambda.runtime.Context
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.RequestContexts
import org.http4k.core.Response
import org.http4k.core.then
import org.http4k.filter.ServerFilters.CatchAll
import org.http4k.filter.ServerFilters.InitialiseRequestContext
import java.io.InputStream

/**
 * Function loader for Invocation Lambdas
 */
class InvocationFunctionLoader(private val appLoader: AppLoaderWithContexts) : FunctionLoader<Context> {
    constructor(input: AppLoader) : this(AppLoaderWithContexts { env, _ -> input(env) })
    constructor(input: HttpHandler) : this(AppLoader { input })

    private val contexts = RequestContexts("lambda")

    override operator fun invoke(env: Map<String, String>): StreamHandler<Context> {
        val app = appLoader(env, contexts)
        return StreamHandler { inputStream, ctx ->
            val request = Request(POST, "/2015-03-31/functions/${ctx.functionName}/invocations")
                .header("X-Amz-Invocation-Type", "RequestResponse")
                .header("X-Amz-Log-Type", "Tail").body(inputStream)
            CatchAll()
                .then(InitialiseRequestContext(contexts))
                .then(AddLambdaContextAndRequest(ctx, request, contexts))
                .then(app)(request)
                .body.stream
        }
    }
}

/**
 * This is the main entry point for lambda invocations using the direct invocations.
 * It uses the local environment to instantiate the HttpHandler which can be used
 * for further invocations.
 */
abstract class InvocationLambdaFunction(appLoader: AppLoaderWithContexts) :
    Http4kRequestHandler(InvocationFunctionLoader(appLoader)) {
    constructor(input: AppLoader) : this(AppLoaderWithContexts { env, _ -> input(env) })
    constructor(input: HttpHandler) : this(AppLoader { input })
}

object InvocationLambdaAwsHttpAdapter : AwsHttpAdapter<InputStream, InputStream> {
    override fun invoke(req: InputStream, ctx: Context) =
        Request(POST, "/2015-03-31/functions/${ctx.functionName}/invocations")
            .header("X-Amz-Invocation-Type", "RequestResponse")
            .header("X-Amz-Log-Type", "Tail").body(req)

    override fun invoke(resp: Response) = resp.body.stream
}
