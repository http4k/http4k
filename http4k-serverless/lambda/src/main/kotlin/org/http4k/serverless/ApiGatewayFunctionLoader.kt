package org.http4k.serverless

import com.amazonaws.services.lambda.runtime.Context
import org.http4k.core.RequestContexts
import org.http4k.core.then
import org.http4k.filter.ServerFilters.CatchAll
import org.http4k.filter.ServerFilters.InitialiseRequestContext
import org.http4k.format.AwsLambdaMoshi.asA
import org.http4k.format.AwsLambdaMoshi.asInputStream

abstract class ApiGatewayFunctionLoader protected constructor(
    private val adapter: AwsHttpAdapter<Map<String, Any>, Map<String, Any>>,
    private val appLoader: AppLoaderWithContexts,
) : FunctionLoader<Context> {
    private val contexts = RequestContexts("lambda")

    override operator fun invoke(env: Map<String, String>): StreamHandler<Context> {
        val app = appLoader(env, contexts)
        return StreamHandler { inputStream, ctx ->
            val newRequest = adapter(asA(inputStream), ctx)
            asInputStream(
                adapter(
                    CatchAll()
                        .then(InitialiseRequestContext(contexts))
                        .then(AddLambdaContextAndRequest(ctx, newRequest, contexts))
                        .then(app)(newRequest)
                )
            )
        }
    }
}
