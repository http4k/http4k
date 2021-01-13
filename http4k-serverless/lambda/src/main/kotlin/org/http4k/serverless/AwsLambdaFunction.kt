package org.http4k.serverless

import com.amazonaws.services.lambda.runtime.Context
import org.http4k.core.Filter
import org.http4k.core.RequestContexts
import org.http4k.core.then
import org.http4k.filter.ServerFilters.CatchAll
import org.http4k.filter.ServerFilters.InitialiseRequestContext

const val LAMBDA_CONTEXT_KEY = "HTTP4K_LAMBDA_CONTEXT"
const val LAMBDA_REQUEST_KEY = "HTTP4K_LAMBDA_REQUEST"

abstract class AwsLambdaFunction<Req : Any, Resp> protected constructor(
    private val adapter: AwsHttpAdapter<Req, Resp>,
    appLoader: AppLoaderWithContexts
) {
    private val contexts = RequestContexts("lambda")
    private val app = appLoader(System.getenv(), contexts)

    internal fun handle(req: Req, ctx: Context): Resp = adapter(
        CatchAll()
            .then(InitialiseRequestContext(contexts))
            .then(AddLambdaContextAndRequest(ctx, req, contexts))
            .then(app)(adapter(req, ctx)))
}

internal fun AddLambdaContextAndRequest(ctx: Context?, request: Any, contexts: RequestContexts) = Filter { next ->
    {
        ctx?.apply { contexts[it][LAMBDA_CONTEXT_KEY] = ctx }
        contexts[it][LAMBDA_REQUEST_KEY] = request
        next(it)
    }
}
