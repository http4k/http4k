package org.http4k.serverless

import com.amazonaws.services.lambda.runtime.Context
import org.http4k.core.RequestContexts
import org.http4k.core.then
import org.http4k.filter.ServerFilters.CatchAll
import org.http4k.filter.ServerFilters.InitialiseRequestContext

abstract class AwsLambdaFunction<Req : Any, Resp> protected constructor(
    private val adapter: AwsHttpAdapter<Req, Resp>,
    appLoader: AppLoaderWithContexts,
    env: Map<String, String> = System.getenv()
) {
    private val contexts = RequestContexts("lambda")
    private val app = appLoader(env, contexts)

    protected fun handle(req: Req, ctx: Context): Resp = adapter(
        CatchAll()
            .then(InitialiseRequestContext(contexts))
            .then(AddLambdaContextAndRequest(ctx, req, contexts))
            .then(app)(adapter(req, ctx))
    )
}
