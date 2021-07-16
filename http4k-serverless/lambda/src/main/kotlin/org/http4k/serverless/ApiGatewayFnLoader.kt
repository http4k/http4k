package org.http4k.serverless

import com.amazonaws.services.lambda.runtime.Context
import com.squareup.moshi.Moshi
import okio.Okio
import okio.buffer
import okio.source
import org.http4k.core.RequestContexts
import org.http4k.core.then
import org.http4k.filter.ServerFilters.CatchAll
import org.http4k.filter.ServerFilters.InitialiseRequestContext
import java.io.InputStream

abstract class ApiGatewayFnLoader protected constructor(
    private val adapter: AwsHttpAdapter<Map<String, Any>, Map<String, Any>>,
    private val appLoader: AppLoaderWithContexts,
) : FnLoader<Context> {
    private val moshi = Moshi.Builder().build()
    private val contexts = RequestContexts("lambda")
    private val coreFilter = CatchAll().then(InitialiseRequestContext(contexts))

    override operator fun invoke(env: Map<String, String>): FnHandler<InputStream, Context, InputStream> {
        val app = appLoader(env, contexts)
        return FnHandler { inputStream, ctx ->
            val newRequest = adapter(moshi.asA(inputStream), ctx)
            moshi.asInputStream(
                adapter(
                    coreFilter
                        .then(AddLambdaContextAndRequest(ctx, newRequest, contexts))
                        .then(app)(newRequest)
                )
            )
        }
    }
}

private inline fun <reified T : Any> Moshi.asA(input: InputStream): T =
    adapter(T::class.java).fromJson(input.source().buffer())!!

private inline fun <reified T> Moshi.asInputStream(a: T) =
    adapter(T::class.java).toJson(a).byteInputStream()
