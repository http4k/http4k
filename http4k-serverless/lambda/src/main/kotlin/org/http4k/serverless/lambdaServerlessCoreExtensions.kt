package org.http4k.serverless

import org.http4k.format.AutoMarshalling
import org.http4k.format.AwsLambdaMoshi
import org.http4k.serverless.FnHandler
import org.http4k.serverless.FnLoader
import java.io.InputStream

/**
 * Convenience DSL for constructing a converting polymorphic FunctionHandler
 */
inline fun <reified In, Ctx, Out : Any> FnLoader(
    autoMarshalling: AutoMarshalling = AwsLambdaMoshi,
    crossinline makeHandler: (Map<String, String>) -> FnHandler<In, Ctx, Out>
): FnLoader<Ctx> = object : FnLoader<Ctx> {
    override fun invoke(env: Map<String, String>): FnHandler<InputStream, Ctx, InputStream> {
        val receiver = makeHandler(env)
        return FnHandler { inputStream, ctx: Ctx ->
            autoMarshalling
                .asFormatString(receiver(autoMarshalling.asA(inputStream), ctx))
                .trimStart('"')
                .trimEnd('"')
                .byteInputStream()
        }
    }
}
