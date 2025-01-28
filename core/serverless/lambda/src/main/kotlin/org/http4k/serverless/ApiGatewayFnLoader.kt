package org.http4k.serverless

import com.amazonaws.services.lambda.runtime.Context
import com.squareup.moshi.Moshi
import okio.buffer
import okio.source
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.then
import org.http4k.filter.ServerFilters.CatchAll
import java.io.InputStream

abstract class ApiGatewayFnLoader protected constructor(
    private val adapter: AwsHttpAdapter<Map<String, Any>, Map<String, Any>>,
    private val appLoader: AppLoader,
) : FnLoader<Context> {
    private val moshi = Moshi.Builder().build()
    private val coreFilter = CatchAll()

    override operator fun invoke(env: Map<String, String>): FnHandler<InputStream, Context, InputStream> {
        val app = appLoader(env)

        return FnHandler { inputStream, ctx ->
            val request = moshi.asA<Map<String, Any>>(inputStream)
            val response = adapter(request, ctx)
                .fold(
                    {
                        coreFilter
                            .then(AddLambdaContextAndRequest(ctx, request))
                            .then(app)(it)
                    },
                    {
                        Response(BAD_REQUEST).body(it.localizedMessage)
                    }
                )
            moshi.asInputStream(adapter(response))
        }
    }
}

private inline fun <reified T : Any> Moshi.asA(input: InputStream): T =
    adapter(T::class.java).fromJson(input.source().buffer())!!

private inline fun <reified T> Moshi.asInputStream(a: T) =
    adapter(T::class.java).toJson(a).byteInputStream()
