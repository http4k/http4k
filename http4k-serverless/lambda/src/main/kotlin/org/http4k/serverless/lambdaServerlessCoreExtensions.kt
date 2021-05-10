package org.http4k.serverless

import org.http4k.format.AutoMarshalling
import org.http4k.format.AwsLambdaMoshi

/**
 * Convenience DSL for constructing a converting polymorphic FunctionHandler
 */
inline fun <reified In, Ctx, Out : Any> FunctionLoader(
    autoMarshalling: AutoMarshalling = AwsLambdaMoshi,
    crossinline makeHandler: (Map<String, String>) -> FnHandler<In, Ctx, Out>
): FnLoader<Ctx> = FnLoader<Ctx> { env ->
    with(makeHandler(env)) {
        FnHandler { inputStream, ctx: Ctx ->
            autoMarshalling
                .asFormatString(this(AwsLambdaMoshi.asA(inputStream), ctx))
                .trimStart('"')
                .trimEnd('"')
                .byteInputStream()
        }
    }
}
