package org.http4k.serverless

import org.http4k.format.AutoMarshalling
import org.http4k.format.AwsLambdaMoshi

/**
 * Convenience DSL for constructing a converting polymorphic FunctionHandler
 */
inline fun <reified In, Ctx, Out : Any> FunctionLoader(
    autoMarshalling: AutoMarshalling = AwsLambdaMoshi,
    crossinline makeHandler: (Map<String, String>) -> FunctionHandler<In, Ctx, Out>
): FunctionLoader<Ctx> = FunctionLoader<Ctx> { env ->
    with(makeHandler(env)) {
        StreamHandler { inputStream, ctx: Ctx ->
            autoMarshalling
                .asFormatString(this(AwsLambdaMoshi.asA(inputStream), ctx))
                .trimStart('"')
                .trimEnd('"')
                .byteInputStream()
        }
    }
}
